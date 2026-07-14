package com.systemdesign.feed.service;

import com.systemdesign.feed.config.KafkaConfig;
import com.systemdesign.feed.model.PostCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("kafka")
public class KafkaProducerService implements PostEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MetricsTracker metricsTracker;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate, MetricsTracker metricsTracker) {
        this.kafkaTemplate = kafkaTemplate;
        this.metricsTracker = metricsTracker;
    }

    @Override
    public void publish(PostCreatedEvent event) {
        log.debug("Publishing event to real Kafka topic: authorId={}", event.getAuthorId());
        metricsTracker.incrementKafkaPublished();
        
        kafkaTemplate.send(KafkaConfig.POST_EVENTS_TOPIC, String.valueOf(event.getAuthorId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event to Kafka", ex);
                    }
                });
    }
}
