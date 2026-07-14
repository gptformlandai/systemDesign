package com.systemdesign.feed.controller;

import com.systemdesign.feed.model.User;
import com.systemdesign.feed.repository.UserRepository;
import com.systemdesign.feed.service.MetricsTracker;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;
    private final MetricsTracker metricsTracker;

    public UserController(UserRepository userRepository, MetricsTracker metricsTracker) {
        this.userRepository = userRepository;
        this.metricsTracker = metricsTracker;
    }

    @PostMapping
    public Mono<User> createUser(@RequestBody User user) {
        metricsTracker.incrementDbWrites(1);
        return userRepository.save(user);
    }

    @GetMapping
    public Flux<User> getAllUsers() {
        metricsTracker.incrementDbReads(1);
        return userRepository.findAll();
    }
}
