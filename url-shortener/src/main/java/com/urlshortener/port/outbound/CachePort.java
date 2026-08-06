package com.urlshortener.port.outbound;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Outbound port for the multi-layer URL mapping cache.
 *
 * <p>Used by the Redirect Service to implement the cache lookup chain:
 * L1 local Caffeine → L2 Redis → L3 metadata store.
 *
 * <p>Cache semantics:
 * <ul>
 *   <li>Positive entries (found mappings): TTL configured per layer</li>
 *   <li>Negative entries (missing/expired codes): shorter TTL to prevent DB hammering</li>
 *   <li>Invalidation: explicit eviction on disable/delete, bounded by TTL otherwise</li>
 * </ul>
 *
 * @param <K> cache key type (typically {@code ShortCode})
 * @param <V> cached value type (typically {@code UrlMapping})
 */
public interface CachePort<K, V> {

    /**
     * Returns the cached value, or empty if not found.
     */
    Mono<V> get(K key);

    /**
     * Stores a value with the given TTL.
     */
    Mono<Void> put(K key, V value, Duration ttl);

    /**
     * Explicitly evicts an entry (e.g., on link disable/delete).
     */
    Mono<Void> evict(K key);
}
