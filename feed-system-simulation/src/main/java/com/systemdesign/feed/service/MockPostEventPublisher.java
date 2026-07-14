package com.systemdesign.feed.service;

import com.systemdesign.feed.model.PostCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Profile("!kafka")
public class MockPostEventPublisher implements PostEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(MockPostEventPublisher.class);
    
    private final FanoutService fanoutService;
    private final MetricsTracker metricsTracker;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public MockPostEventPublisher(FanoutService fanoutService, MetricsTracker metricsTracker) {
        this.fanoutService = fanoutService;
        this.metricsTracker = metricsTracker;
        log.info("Initialized in-memory Mock Kafka Event Bus (Zero dependencies mode)");
    }

    @Override
    public void publish(PostCreatedEvent event) {
        log.debug("Routing post created event asynchronously through Mock Event Bus...");
        
        metricsTracker.incrementKafkaPublished();
        
        executor.submit(() -> {
            try {
                // Simulate network transit latency of Kafka queue
                Thread.sleep(5); 
                
                metricsTracker.incrementKafkaConsumed();
                
                fanoutService.fanoutPost(event.getPostId(), event.getAuthorId(), event.isCelebrity())
                        .block();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Mock Event Bus delivery interrupted", e);
            }
        });
    }
}
