package com.systemdesign.feed.service;

import com.systemdesign.feed.model.FeedInbox;
import com.systemdesign.feed.model.Follow;
import com.systemdesign.feed.repository.FeedInboxRepository;
import com.systemdesign.feed.repository.FollowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Service
public class FanoutService {
    private static final Logger log = LoggerFactory.getLogger(FanoutService.class);
    
    private final FollowRepository followRepository;
    private final FeedInboxRepository feedInboxRepository;
    private final MetricsTracker metricsTracker;
    
    // Toggled dynamically by simulation or configuration
    private volatile String activeStrategy = "HYBRID";

    public FanoutService(FollowRepository followRepository, 
                         FeedInboxRepository feedInboxRepository, 
                         MetricsTracker metricsTracker) {
        this.followRepository = followRepository;
        this.feedInboxRepository = feedInboxRepository;
        this.metricsTracker = metricsTracker;
    }

    public String getActiveStrategy() {
        return activeStrategy;
    }

    public void setActiveStrategy(String activeStrategy) {
        this.activeStrategy = activeStrategy;
        log.info("Active fanout strategy changed to: {}", activeStrategy);
    }

    public Mono<Void> fanoutPost(Long postId, Long authorId, boolean isCelebrity) {
        String strategy = this.activeStrategy;
        
        if ("PULL".equalsIgnoreCase(strategy)) {
            log.debug("Strategy is PULL. Skipping write-time fanout for post {}", postId);
            return Mono.empty();
        }
        
        if ("HYBRID".equalsIgnoreCase(strategy) && isCelebrity) {
            log.debug("Strategy is HYBRID and author {} is celebrity. Skipping write-time fanout for post {}", authorId, postId);
            return Mono.empty();
        }

        log.debug("Executing fanout for post {} by author {} (strategy={})", postId, authorId, strategy);
        
        // Count the follow lookup DB read operation
        metricsTracker.incrementDbReads(1);
        
        long startTime = System.currentTimeMillis();
        
        return followRepository.findAllByFollowedId(authorId)
                .map(Follow::getFollowerId)
                .flatMap(followerId -> {
                    FeedInbox feedInboxItem = FeedInbox.builder()
                            .viewerId(followerId)
                            .postId(postId)
                            .authorId(authorId)
                            .createdAt(LocalDateTime.now())
                            .build();
                    
                    // Increment write counter
                    metricsTracker.incrementDbWrites(1);
                    
                    return feedInboxRepository.save(feedInboxItem)
                            .onErrorResume(ex -> {
                                log.debug("Duplicate inbox write ignored: viewer={}, post={}", followerId, postId);
                                return Mono.empty(); // Ignore duplicates
                            });
                })
                .then()
                .doOnSuccess(v -> {
                    long duration = System.currentTimeMillis() - startTime;
                    metricsTracker.recordWriteLatency("FANOUT_" + strategy, duration);
                    log.debug("Fanout complete for post {} in {}ms", postId, duration);
                });
    }
}
