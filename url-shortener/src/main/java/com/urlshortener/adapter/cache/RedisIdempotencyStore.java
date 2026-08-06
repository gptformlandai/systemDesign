package com.urlshortener.adapter.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.domain.IdempotencyRecord;
import com.urlshortener.domain.ShortCode;
import com.urlshortener.service.UrlShorteningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis-backed idempotency store for the URL creation flow.
 *
 * <p>Key format: {@code url:idempotency:{userId}:{idempotencyKey}}
 * This scoping ensures idempotency keys are user-specific (one user's key
 * cannot clash with another user's key).
 *
 * <p>TTL: configurable via {@code app.idempotency.ttl} (default 24 hours).
 */
@Slf4j
@Component
public class RedisIdempotencyStore implements UrlShorteningService.IdempotencyStore {

    private static final String KEY_PREFIX = "url:idempotency:";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisIdempotencyStore(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.idempotency.ttl:PT24H}") Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public Mono<IdempotencyRecord> get(String key, String userId) {
        return redisTemplate.opsForValue()
                .get(toRedisKey(key, userId))
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, IdempotencyRecord.class));
                    } catch (Exception e) {
                        log.warn("Failed to deserialize idempotency record for key={}", key, e);
                        return Mono.empty();
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Redis GET failed for idempotency key={}: {}", key, e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> save(IdempotencyRecord record) {
        try {
            String json = objectMapper.writeValueAsString(record);
            return redisTemplate.opsForValue()
                    .set(toRedisKey(record.idempotencyKey(), record.userId()), json, ttl)
                    .then()
                    .onErrorResume(e -> {
                        log.warn("Failed to save idempotency record for key={}: {}",
                                record.idempotencyKey(), e.getMessage());
                        return Mono.empty();
                    });
        } catch (Exception e) {
            log.error("Serialization failed for idempotency record", e);
            return Mono.empty();
        }
    }

    private String toRedisKey(String key, String userId) {
        return KEY_PREFIX + userId + ":" + key;
    }
}
