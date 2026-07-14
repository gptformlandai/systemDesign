package com.systemdesign.feed.service;

import com.systemdesign.feed.model.Follow;
import com.systemdesign.feed.model.Post;
import com.systemdesign.feed.model.User;
import com.systemdesign.feed.repository.FeedInboxRepository;
import com.systemdesign.feed.repository.FollowRepository;
import com.systemdesign.feed.repository.PostRepository;
import com.systemdesign.feed.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;

@Service
public class FeedQueryService {
    private static final Logger log = LoggerFactory.getLogger(FeedQueryService.class);

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final FeedInboxRepository feedInboxRepository;
    private final MetricsTracker metricsTracker;

    public FeedQueryService(UserRepository userRepository,
                            PostRepository postRepository,
                            FollowRepository followRepository,
                            FeedInboxRepository feedInboxRepository,
                            MetricsTracker metricsTracker) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.followRepository = followRepository;
        this.feedInboxRepository = feedInboxRepository;
        this.metricsTracker = metricsTracker;
    }

    public Flux<Post> getFeed(Long viewerId, String strategy) {
        long startTime = System.currentTimeMillis();
        Flux<Post> feedFlux;

        if ("PUSH".equalsIgnoreCase(strategy)) {
            feedFlux = getPushFeed(viewerId);
        } else if ("PULL".equalsIgnoreCase(strategy)) {
            feedFlux = getPullFeed(viewerId);
        } else {
            feedFlux = getHybridFeed(viewerId);
        }

        return feedFlux
                .collectList()
                .flatMapMany(posts -> {
                    long latency = System.currentTimeMillis() - startTime;
                    metricsTracker.recordReadLatency(strategy.toUpperCase(), latency);
                    log.debug("Fetched {} feed items for user {} using strategy {} in {}ms", 
                            posts.size(), viewerId, strategy, latency);
                    return Flux.fromIterable(posts);
                });
    }

    /**
     * PUSH: Fetch precomputed list of post IDs from FeedInbox, then hydrate.
     * DB Reads: 1 (inbox scan) + N (hydrate posts)
     */
    private Flux<Post> getPushFeed(Long viewerId) {
        metricsTracker.incrementDbReads(1); // Read inbox
        
        return feedInboxRepository.findAllByViewerIdOrderByCreatedAtDesc(viewerId)
                .flatMap(inboxItem -> {
                    metricsTracker.incrementDbReads(1); // Hydrate post body
                    return postRepository.findById(inboxItem.getPostId());
                });
    }

    /**
     * PULL: Fetch follow list, fetch recent posts from all followed authors, and merge/sort in memory.
     * DB Reads: 1 (follow list scan) + 1 (batch fetch posts)
     */
    private Flux<Post> getPullFeed(Long viewerId) {
        metricsTracker.incrementDbReads(1); // Read follow list
        
        return followRepository.findAllByFollowerId(viewerId)
                .map(Follow::getFollowedId)
                .collectList()
                .flatMapMany(followedIds -> {
                    if (followedIds.isEmpty()) {
                        return Flux.empty();
                    }
                    metricsTracker.incrementDbReads(1); // Fetch followed users' posts
                    return postRepository.findAllByAuthorIdIn(followedIds)
                            .sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));
                });
    }

    /**
     * HYBRID: 
     * 1. Get followed user IDs.
     * 2. Identify which followed accounts are celebrities vs normal.
     * 3. Fetch precomputed inbox (contains normal users' posts).
     * 4. Fetch timelines of celebrities directly (pulling on-the-fly).
     * 5. Merge-sort both streams in memory.
     * DB Reads: 1 (follow graph) + 1 (identify celebrity status) + 1 (inbox scan) + N (hydrate inbox posts) + 1 (celebrity timeline scan)
     */
    private Flux<Post> getHybridFeed(Long viewerId) {
        metricsTracker.incrementDbReads(1); // Read follow graph
        
        return followRepository.findAllByFollowerId(viewerId)
                .map(Follow::getFollowedId)
                .collectList()
                .flatMapMany(followedIds -> {
                    if (followedIds.isEmpty()) {
                        return Flux.empty();
                    }
                    
                    metricsTracker.incrementDbReads(1); // Check celebrity statuses
                    return userRepository.findAllById(followedIds)
                            .collectList()
                            .flatMapMany(followedUsers -> {
                                List<Long> celebrityIds = followedUsers.stream()
                                        .filter(User::isCelebrity)
                                        .map(User::getId)
                                        .toList();
                                
                                metricsTracker.incrementDbReads(1); // Read inbox scan
                                Flux<Post> normalPushedPosts = feedInboxRepository.findAllByViewerIdOrderByCreatedAtDesc(viewerId)
                                        .flatMap(inboxItem -> {
                                            metricsTracker.incrementDbReads(1); // Hydrate normal post body
                                            return postRepository.findById(inboxItem.getPostId());
                                        });

                                Flux<Post> celebrityPulledPosts = Flux.empty();
                                if (!celebrityIds.isEmpty()) {
                                    metricsTracker.incrementDbReads(1); // Read celebrity timelines
                                    celebrityPulledPosts = postRepository.findAllByAuthorIdIn(celebrityIds);
                                }

                                return Flux.merge(normalPushedPosts, celebrityPulledPosts)
                                        .distinct(Post::getId) // Prevent duplication
                                        .sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));
                            });
                });
    }
}
