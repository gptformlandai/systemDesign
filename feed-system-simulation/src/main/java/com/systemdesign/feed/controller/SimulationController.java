package com.systemdesign.feed.controller;

import com.systemdesign.feed.model.Follow;
import com.systemdesign.feed.model.Post;
import com.systemdesign.feed.model.User;
import com.systemdesign.feed.repository.FeedInboxRepository;
import com.systemdesign.feed.repository.FollowRepository;
import com.systemdesign.feed.repository.PostRepository;
import com.systemdesign.feed.repository.UserRepository;
import com.systemdesign.feed.service.FanoutService;
import com.systemdesign.feed.service.FeedQueryService;
import com.systemdesign.feed.service.MetricsTracker;
import com.systemdesign.feed.controller.PostController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/simulate")
public class SimulationController {
    private static final Logger log = LoggerFactory.getLogger(SimulationController.class);

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final FeedInboxRepository feedInboxRepository;
    private final FanoutService fanoutService;
    private final FeedQueryService feedQueryService;
    private final PostController postController;
    private final MetricsTracker metricsTracker;

    public SimulationController(UserRepository userRepository,
                                PostRepository postRepository,
                                FollowRepository followRepository,
                                FeedInboxRepository feedInboxRepository,
                                FanoutService fanoutService,
                                FeedQueryService feedQueryService,
                                PostController postController,
                                MetricsTracker metricsTracker) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.followRepository = followRepository;
        this.feedInboxRepository = feedInboxRepository;
        this.fanoutService = fanoutService;
        this.feedQueryService = feedQueryService;
        this.postController = postController;
        this.metricsTracker = metricsTracker;
    }

    @PostMapping("/reset")
    public Mono<String> resetSystem() {
        metricsTracker.reset();
        return feedInboxRepository.deleteAll()
                .then(followRepository.deleteAll())
                .then(postRepository.deleteAll())
                .then(userRepository.deleteAll())
                .then(Mono.just("System database and metrics cleared."));
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("dbReads", metricsTracker.getDbReads().get());
        response.put("dbWrites", metricsTracker.getDbWrites().get());
        response.put("kafkaEventsPublished", metricsTracker.getKafkaEventsPublished().get());
        response.put("kafkaEventsConsumed", metricsTracker.getKafkaEventsConsumed().get());
        
        Map<String, Object> readLats = new LinkedHashMap<>();
        metricsTracker.getReadLatencies().forEach((k, v) -> {
            if (!v.isEmpty()) {
                double avg = v.stream().mapToLong(Long::longValue).average().orElse(0.0);
                long max = v.stream().mapToLong(Long::longValue).max().orElse(0L);
                readLats.put(k, Map.of("avgMs", String.format("%.2f", avg), "maxMs", max, "requestsCount", v.size()));
            }
        });
        response.put("readLatencies", readLats);

        Map<String, Object> writeLats = new LinkedHashMap<>();
        metricsTracker.getWriteLatencies().forEach((k, v) -> {
            if (!v.isEmpty()) {
                double avg = v.stream().mapToLong(Long::longValue).average().orElse(0.0);
                writeLats.put(k, Map.of("avgMs", String.format("%.2f", avg), "writesCount", v.size()));
            }
        });
        response.put("writeLatencies", writeLats);

        return response;
    }

    @PostMapping
    public Mono<Map<String, Object>> runSimulation(@RequestParam(value = "users", defaultValue = "50") int totalUsers,
                                                   @RequestParam(value = "celebrities", defaultValue = "2") int totalCelebrities,
                                                   @RequestParam(value = "posts", defaultValue = "100") int totalPosts) {
        log.info("Starting simulation: users={}, celebrities={}, posts={}", totalUsers, totalCelebrities, totalPosts);
        
        Map<String, Object> report = new LinkedHashMap<>();
        
        // Step 1: Clear everything
        return resetSystem()
                .then(seedUsersAndFollows(totalUsers, totalCelebrities))
                .flatMap(usersMap -> {
                    List<User> normalUsers = usersMap.get("normal");
                    List<User> celebs = usersMap.get("celebrity");
                    List<User> allUsers = new ArrayList<>();
                    allUsers.addAll(normalUsers);
                    allUsers.addAll(celebs);
                    
                    // Run benchmark for PULL strategy
                    return benchmarkStrategy("PULL", normalUsers, celebs, allUsers, totalPosts)
                            .flatMap(pullResults -> {
                                report.put("PULL_Strategy", pullResults);
                                
                                // Run benchmark for PUSH strategy
                                return benchmarkStrategy("PUSH", normalUsers, celebs, allUsers, totalPosts)
                                        .flatMap(pushResults -> {
                                            report.put("PUSH_Strategy", pushResults);
                                            
                                            // Run benchmark for HYBRID strategy
                                            return benchmarkStrategy("HYBRID", normalUsers, celebs, allUsers, totalPosts)
                                                    .flatMap(hybridResults -> {
                                                        report.put("HYBRID_Strategy", hybridResults);
                                                        
                                                        // Print summary table to console
                                                        logSummaryTable(report);
                                                        
                                                        return Mono.just(report);
                                                    });
                                        });
                            });
                });
    }

    private Mono<Map<String, List<User>>> seedUsersAndFollows(int totalUsers, int totalCelebrities) {
        log.info("Seeding follow graph...");
        List<Mono<User>> userSaves = new ArrayList<>();
        
        // 1. Create Celebrities
        for (int i = 1; i <= totalCelebrities; i++) {
            User celeb = User.builder()
                    .username("celebrity_" + i)
                    .isCelebrity(true)
                    .build();
            userSaves.add(userRepository.save(celeb));
        }
        
        // 2. Create Normal Users
        for (int i = 1; i <= totalUsers; i++) {
            User normal = User.builder()
                    .username("user_" + i)
                    .isCelebrity(false)
                    .build();
            userSaves.add(userRepository.save(normal));
        }

        return Flux.merge(userSaves)
                .collectList()
                .flatMap(users -> {
                    List<User> celebs = users.stream().filter(User::isCelebrity).toList();
                    List<User> normal = users.stream().filter(u -> !u.isCelebrity()).toList();
                    
                    List<Mono<Follow>> followSaves = new ArrayList<>();
                    
                    // All normal users follow all celebrities
                    for (User normalUser : normal) {
                        for (User celeb : celebs) {
                            Follow follow = Follow.builder()
                                    .followerId(normalUser.getId())
                                    .followedId(celeb.getId())
                                    .build();
                            followSaves.add(followRepository.save(follow));
                        }
                    }
                    
                    // Create random follow links between normal users (average 5 followings each)
                    Random random = new Random(42);
                    for (User normalUser : normal) {
                        int followingsCount = 3 + random.nextInt(5);
                        Set<Long> followedIds = new HashSet<>();
                        while (followedIds.size() < followingsCount) {
                            User candidate = normal.get(random.nextInt(normal.size()));
                            if (!candidate.getId().equals(normalUser.getId())) {
                                followedIds.add(candidate.getId());
                            }
                        }
                        for (Long followedId : followedIds) {
                            Follow follow = Follow.builder()
                                    .followerId(normalUser.getId())
                                    .followedId(followedId)
                                    .build();
                            followSaves.add(followRepository.save(follow));
                        }
                    }

                    return Flux.merge(followSaves)
                            .then(Mono.just(Map.of("celebrity", celebs, "normal", normal)));
                });
    }

    private Mono<Map<String, Object>> benchmarkStrategy(String strategy,
                                                         List<User> normalUsers,
                                                         List<User> celebs,
                                                         List<User> allUsers,
                                                         int totalPosts) {
        log.info("Benchmarking strategy: {}", strategy);
        fanoutService.setActiveStrategy(strategy);
        metricsTracker.reset();

        // 1. Generate Posts (Write path workload)
        // 20% written by celebrities, 80% written by normal users
        Random random = new Random();
        List<Mono<Post>> postSaves = new ArrayList<>();
        
        for (int i = 0; i < totalPosts; i++) {
            User author;
            if (random.nextDouble() < 0.20 && !celebs.isEmpty()) {
                author = celebs.get(random.nextInt(celebs.size()));
            } else {
                author = normalUsers.get(random.nextInt(normalUsers.size()));
            }
            
            Post postRequest = Post.builder()
                    .authorId(author.getId())
                    .content("Simulated post " + i + " by " + author.getUsername())
                    .build();
            
            postSaves.add(postController.createPost(postRequest));
        }

        long startWritesTime = System.currentTimeMillis();
        
        return Flux.concat(postSaves) // Execute sequentially to simulate throughput
                .collectList()
                .flatMap(posts -> {
                    long writesCompletedTime = System.currentTimeMillis() - startWritesTime;
                    
                    // Wait for Kafka to consume all events and complete fanout updates
                    return waitForKafka()
                            .then(Mono.defer(() -> {
                                long totalWritesTime = System.currentTimeMillis() - startWritesTime;
                                log.info("Writes and fanout for {} completed in {}ms", strategy, totalWritesTime);
                                
                                // Reset read operations counters so we only measure reads
                                long postWriteDbOps = metricsTracker.getDbWrites().get();
                                long postReadDbOps = metricsTracker.getDbReads().get();
                                
                                metricsTracker.getDbReads().set(0);
                                metricsTracker.getDbWrites().set(0);
                                
                                // 2. Retrieve Feeds (Read path workload)
                                // Query feed of every normal user 5 times to gather latency stats
                                List<Mono<List<Post>>> feedReads = new ArrayList<>();
                                for (User viewer : normalUsers) {
                                    for (int r = 0; r < 5; r++) {
                                        feedReads.add(feedQueryService.getFeed(viewer.getId(), strategy).collectList());
                                    }
                                }
                                
                                long startReadsTime = System.currentTimeMillis();
                                return Flux.merge(feedReads)
                                        .collectList()
                                        .map(feeds -> {
                                            long readsTime = System.currentTimeMillis() - startReadsTime;
                                            
                                            // Compile stats
                                            Map<String, Object> stats = new LinkedHashMap<>();
                                            stats.put("strategy", strategy);
                                            stats.put("totalPostsWritten", totalPosts);
                                            stats.put("postCreateDbWrites", postWriteDbOps);
                                            stats.put("postCreateDbReads", postReadDbOps);
                                            
                                            long readCount = metricsTracker.getReadLatencies().getOrDefault(strategy, List.of()).size();
                                            double avgReadMs = metricsTracker.getReadLatencies().getOrDefault(strategy, List.of())
                                                    .stream().mapToLong(Long::longValue).average().orElse(0.0);
                                            
                                            stats.put("totalFeedReadsTriggered", readCount);
                                            stats.put("feedReadDbReads", metricsTracker.getDbReads().get());
                                            stats.put("avgFeedReadLatencyMs", String.format("%.2f", avgReadMs));
                                            
                                            // Clean databases for the next run
                                            return stats;
                                        });
                            }))
                            .flatMap(stats -> {
                                // Clear inboxes to avoid carry-over
                                return feedInboxRepository.deleteAll()
                                        .then(postRepository.deleteAll())
                                        .then(Mono.just(stats));
                            });
                });
    }

    private Mono<Void> waitForKafka() {
        return Mono.defer(() -> {
            long published = metricsTracker.getKafkaEventsPublished().get();
            long consumed = metricsTracker.getKafkaEventsConsumed().get();
            if (consumed >= published) {
                return Mono.empty();
            }
            log.debug("Waiting for Kafka consumer... published={}, consumed={}", published, consumed);
            return Mono.delay(Duration.ofMillis(100))
                    .then(waitForKafka());
        });
    }

    private void logSummaryTable(Map<String, Object> report) {
        System.out.println("\n==========================================================================");
        System.out.println("                   SOCIAL FEED DISTRIBUTION SYSTEM BENCHMARKS");
        System.out.println("==========================================================================");
        System.out.printf("%-10s | %-16s | %-16s | %-16s | %-12s\n", 
                "STRATEGY", "WRITE DB WRITES", "READ DB READS", "READ LATENCY (AVG)", "KAFKA EVENTS");
        System.out.println("--------------------------------------------------------------------------");
        
        report.forEach((k, v) -> {
            Map<?, ?> stats = (Map<?, ?>) v;
            System.out.printf("%-10s | %-16s | %-16s | %-18s | %-12s\n",
                    stats.get("strategy"),
                    stats.get("postCreateDbWrites"),
                    stats.get("feedReadDbReads"),
                    stats.get("avgFeedReadLatencyMs") + " ms",
                    metricsTracker.getKafkaEventsPublished().get());
        });
        System.out.println("==========================================================================\n");
    }
}
