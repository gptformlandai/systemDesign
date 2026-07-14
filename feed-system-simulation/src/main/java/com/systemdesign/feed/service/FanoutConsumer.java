package com.systemdesign.feed.service;

import com.systemdesign.feed.config.KafkaConfig;
import com.systemdesign.feed.model.PostCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Profile("kafka")
public class FanoutConsumer {
    private static final Logger log = LoggerFactory.getLogger(FanoutConsumer.class);
    
    private final FanoutService fanoutService;
    private final MetricsTracker metricsTracker;

    public FanoutConsumer(FanoutService fanoutService, MetricsTracker metricsTracker) {
        this.fanoutService = fanoutService;
        this.metricsTracker = metricsTracker;
    }

    @KafkaListener(topics = KafkaConfig.POST_EVENTS_TOPIC, groupId = "feed-fanout-group")
    public void consumePostEvent(PostCreatedEvent event) {
        log.debug("Received post created event via Kafka: postId={}, authorId={}, isCelebrity={}", 
                event.getPostId(), event.getAuthorId(), event.isCelebrity());
        
        metricsTracker.incrementKafkaConsumed();
        
        // Execute the fanout and wait for completion
        fanoutService.fanoutPost(event.getPostId(), event.getAuthorId(), event.isCelebrity())
                .block();
    }
}
