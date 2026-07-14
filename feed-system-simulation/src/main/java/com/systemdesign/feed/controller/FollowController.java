package com.systemdesign.feed.controller;

import com.systemdesign.feed.model.Follow;
import com.systemdesign.feed.repository.FollowRepository;
import com.systemdesign.feed.service.MetricsTracker;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/follow")
public class FollowController {
    private final FollowRepository followRepository;
    private final MetricsTracker metricsTracker;

    public FollowController(FollowRepository followRepository, MetricsTracker metricsTracker) {
        this.followRepository = followRepository;
        this.metricsTracker = metricsTracker;
    }

    @PostMapping
    public Mono<Follow> follow(@RequestBody Follow follow) {
        metricsTracker.incrementDbWrites(1);
        return followRepository.save(follow);
    }
}
