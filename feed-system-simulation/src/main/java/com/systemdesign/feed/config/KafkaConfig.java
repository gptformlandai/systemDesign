package com.systemdesign.feed.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("kafka")
public class KafkaConfig {

    public static final String POST_EVENTS_TOPIC = "post-events";

    @Bean
    public NewTopic postEventsTopic() {
        return TopicBuilder.name(POST_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
