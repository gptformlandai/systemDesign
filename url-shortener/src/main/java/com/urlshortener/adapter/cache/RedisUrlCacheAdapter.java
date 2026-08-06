package com.urlshortener.adapter.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.domain.ShortCode;
import com.urlshortener.domain.UrlMapping;
import com.urlshortener.port.outbound.CachePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * L2 Redis cache adapter implementing the {@link CachePort} port.
 *
 * <p>Uses Lettuce (reactive) under the hood via {@link ReactiveStringRedisTemplate}.
 * Values are JSON-serialized to allow schema inspection in Redis without special tooling.
 *
 * <p>Cache key format: {@code url:mapping:{code}} — namespaced to allow
 * multiple cache types to coexist in the same Redis instance.
 *
 * <p>Negative caching: when the redirect service gets an empty result from the
 * metadata store, it stores a sentinel value under the same key with a short TTL
 * (30s) to prevent repeated DB hits from random-code scans.
 */
@Slf4j
@Component
public class RedisUrlCacheAdapter implements CachePort<ShortCode, UrlMapping> {

    private static final String KEY_PREFIX = "url:mapping:";
    private static final String NEGATIVE_SENTINEL = "__NOT_FOUND__";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisUrlCacheAdapter(ReactiveStringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<UrlMapping> get(ShortCode key) {
        return redisTemplate.opsForValue()
                .get(toRedisKey(key))
                .flatMap(json -> {
                    if (NEGATIVE_SENTINEL.equals(json)) {
                        return Mono.empty(); // Negative cache hit — signal "not found"
                    }
                    try {
                        return Mono.just(objectMapper.readValue(json, UrlMapping.class));
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to deserialize cached mapping for code={}", key.value(), e);
                        return Mono.empty(); // Degrade gracefully — fall through to DB
                    }
                })
                .doOnSuccess(m -> {
                    if (m != null) log.debug("Cache HIT for code={}", key.value());
                })
                .doOnError(e -> log.warn("Redis GET failed for code={}: {}", key.value(), e.getMessage()));
    }

    @Override
    public Mono<Void> put(ShortCode key, UrlMapping value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            return redisTemplate.opsForValue()
                    .set(toRedisKey(key), json, ttl)
                    .then()
                    .doOnError(e -> log.warn("Redis SET failed for code={}: {}", key.value(), e.getMessage()))
                    .onErrorResume(e -> Mono.empty()); // Cache failure must not affect callers
        } catch (JsonProcessingException e) {
            log.error("Serialization error for code={}", key.value(), e);
            return Mono.empty();
        }
    }

    @Override
    public Mono<Void> evict(ShortCode key) {
        return redisTemplate.opsForValue()
                .delete(toRedisKey(key))
                .then()
                .doOnError(e -> log.warn("Redis DEL failed for code={}: {}", key.value(), e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * Stores a negative cache sentinel so repeated lookups for a missing code
     * skip the metadata store during the TTL window.
     */
    public Mono<Void> putNegative(ShortCode key, Duration ttl) {
        return redisTemplate.opsForValue()
                .set(toRedisKey(key), NEGATIVE_SENTINEL, ttl)
                .then()
                .onErrorResume(e -> Mono.empty());
    }

    private String toRedisKey(ShortCode key) {
        return KEY_PREFIX + key.value();
    }
}
