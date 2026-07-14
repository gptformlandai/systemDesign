package com.systemdesign.feed.controller;

import com.systemdesign.feed.model.Post;
import com.systemdesign.feed.model.PostCreatedEvent;
import com.systemdesign.feed.repository.PostRepository;
import com.systemdesign.feed.repository.UserRepository;
import com.systemdesign.feed.service.PostEventPublisher;
import com.systemdesign.feed.service.MetricsTracker;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostEventPublisher postEventPublisher;
    private final MetricsTracker metricsTracker;

    public PostController(PostRepository postRepository,
                          UserRepository userRepository,
                          PostEventPublisher postEventPublisher,
                          MetricsTracker metricsTracker) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postEventPublisher = postEventPublisher;
        this.metricsTracker = metricsTracker;
    }

    @PostMapping
    public Mono<Post> createPost(@RequestBody Post postRequest) {
        long startTime = System.currentTimeMillis();
        
        postRequest.setCreatedAt(LocalDateTime.now());
        metricsTracker.incrementDbWrites(1);
        
        return postRepository.save(postRequest)
                .flatMap(savedPost -> {
                    metricsTracker.incrementDbReads(1);
                    return userRepository.findById(savedPost.getAuthorId())
                            .map(user -> {
                                PostCreatedEvent event = PostCreatedEvent.builder()
                                        .postId(savedPost.getId())
                                        .authorId(savedPost.getAuthorId())
                                        .isCelebrity(user.isCelebrity())
                                        .createdAt(savedPost.getCreatedAt())
                                        .build();
                                
                                postEventPublisher.publish(event);
                                
                                long latency = System.currentTimeMillis() - startTime;
                                metricsTracker.recordWriteLatency("CREATE_POST", latency);
                                
                                return savedPost;
                            });
                });
    }
}
