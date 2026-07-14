package com.systemdesign.feed.controller;

import com.systemdesign.feed.model.Post;
import com.systemdesign.feed.service.FeedQueryService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/feed")
public class FeedController {
    private final FeedQueryService feedQueryService;

    public FeedController(FeedQueryService feedQueryService) {
        this.feedQueryService = feedQueryService;
    }

    @GetMapping("/{userId}")
    public Flux<Post> getFeed(@PathVariable Long userId, 
                              @RequestParam(value = "strategy", defaultValue = "HYBRID") String strategy) {
        return feedQueryService.getFeed(userId, strategy);
    }
}
