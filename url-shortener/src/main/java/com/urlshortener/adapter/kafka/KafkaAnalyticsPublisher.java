package com.urlshortener.adapter.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.domain.ClickEvent;
import com.urlshortener.port.outbound.AnalyticsPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

/**
 * Reactor Kafka adapter implementing {@link AnalyticsPublisher}.
 *
 * <p>Topic: {@code url-shortener.click-events}<br>
 * Key: {@code code} — ensures all events for the same short code
 * land on the same partition (locality for stream aggregation).
 *
 * <p>Failure contract:
 * <ul>
 *   <li>Kafka producer errors are logged but NOT propagated back — the redirect
 *       path must never fail because of analytics.</li>
 *   <li>A Resilience4j circuit breaker wraps this publisher at the service layer.</li>
 * </ul>
 */
@Slf4j
@Component
public class KafkaAnalyticsPublisher implements AnalyticsPublisher {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaAnalyticsPublisher(
            KafkaSender<String, String> kafkaSender,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.click-events:url-shortener.click-events}") String topic) {
        this.kafkaSender = kafkaSender;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public Mono<Void> publish(ClickEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ClickEvent for code={}", event.code(), e);
            return Mono.empty(); // Degrade gracefully — do not fail redirect
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.code(), payload);
        SenderRecord<String, String, String> senderRecord =
                SenderRecord.create(record, event.requestId()); // correlationMetadata = requestId

        return kafkaSender.send(Mono.just(senderRecord))
                .doOnNext(result -> {
                    if (result.exception() != null) {
                        log.warn("Kafka send failed for code={} requestId={}: {}",
                                event.code(), event.requestId(), result.exception().getMessage());
                    } else {
                        log.debug("Click event published code={} partition={} offset={}",
                                event.code(),
                                result.recordMetadata().partition(),
                                result.recordMetadata().offset());
                    }
                })
                .then()
                .onErrorResume(e -> {
                    log.error("Unexpected error publishing analytics event for code={}: {}",
                            event.code(), e.getMessage());
                    return Mono.empty(); // Never propagate to redirect path
                });
    }
}
