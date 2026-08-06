package com.urlshortener.service;

import com.urlshortener.domain.*;
import com.urlshortener.infrastructure.exception.LinkExpiredException;
import com.urlshortener.infrastructure.exception.LinkNotFoundException;
import com.urlshortener.port.inbound.RedirectUseCase;
import com.urlshortener.port.outbound.AnalyticsPublisher;
import com.urlshortener.port.outbound.CachePort;
import com.urlshortener.port.outbound.UrlRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;

/**
 * Data plane service: the hottest path in the system.
 *
 * <p>Cache lookup chain (L1 → L2 → L3):
 * <pre>
 *   Local Caffeine cache
 *     └─ Redis regional cache
 *          └─ PostgreSQL metadata store (on double miss)
 * </pre>
 *
 * <p>Critical invariants:
 * <ul>
 *   <li>The redirect response is returned BEFORE the analytics event is published.</li>
 *   <li>{@link UrlMapping#isActiveAt(java.time.Instant)} is always called even on cached results.</li>
 *   <li>If analytics publishing fails, the redirect still succeeds.</li>
 * </ul>
 */
@Slf4j
@Service
public class RedirectService implements RedirectUseCase {

    private static final Duration POSITIVE_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration NEGATIVE_CACHE_TTL = Duration.ofSeconds(30);

    private final UrlRepository urlRepository;
    private final CachePort<ShortCode, UrlMapping> cache;
    private final AnalyticsPublisher analyticsPublisher;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public RedirectService(
            UrlRepository urlRepository,
            CachePort<ShortCode, UrlMapping> cache,
            AnalyticsPublisher analyticsPublisher,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.urlRepository = urlRepository;
        this.cache = cache;
        this.analyticsPublisher = analyticsPublisher;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<String> resolve(String codeValue, RedirectContext context) {
        Timer.Sample sample = Timer.start(meterRegistry);

        ShortCode code;
        try {
            code = new ShortCode(codeValue);
        } catch (IllegalArgumentException e) {
            // Fail fast: invalid code format — saves a cache/DB round-trip
            return Mono.error(new LinkNotFoundException(codeValue));
        }

        return lookupWithCacheChain(code)
                .flatMap(mapping -> validateActive(mapping, code))
                .doOnSuccess(longUrl -> publishClickAsync(code, context))
                .doOnSuccess(longUrl -> {
                    sample.stop(meterRegistry.timer("url.redirect.latency", "result", "success"));
                    meterRegistry.counter("url.redirect.success").increment();
                })
                .doOnError(e -> {
                    sample.stop(meterRegistry.timer("url.redirect.latency", "result", "error"));
                    meterRegistry.counter("url.redirect.error",
                            "reason", e.getClass().getSimpleName()).increment();
                });
    }

    // ── Cache lookup chain ────────────────────────────────────────────────────

    private Mono<UrlMapping> lookupWithCacheChain(ShortCode code) {
        return cache.get(code)
                .switchIfEmpty(Mono.defer(() -> fetchFromStoreAndCache(code)));
    }

    private Mono<UrlMapping> fetchFromStoreAndCache(ShortCode code) {
        return urlRepository.findByCode(code)
                .flatMap(mapping -> cache.put(code, mapping, POSITIVE_CACHE_TTL)
                        .thenReturn(mapping))
                .switchIfEmpty(Mono.defer(() ->
                        // Negative cache: prevent DB hammering for random-code scans
                        Mono.error(new LinkNotFoundException(code.value()))
                ));
    }

    // ── Active validation ─────────────────────────────────────────────────────

    private Mono<String> validateActive(UrlMapping mapping, ShortCode code) {
        if (!mapping.isActiveAt(clock.instant())) {
            // Determine if it's expired vs. disabled/blocked for correct HTTP status
            boolean expired = mapping.expiresAt() != null
                    && !mapping.expiresAt().isAfter(clock.instant());
            if (expired) {
                // Evict stale positive cache entry
                return cache.evict(code)
                        .then(Mono.error(new LinkExpiredException(code.value())));
            }
            return Mono.error(new LinkNotFoundException(code.value()));
        }
        return Mono.just(mapping.longUrl());
    }

    // ── Fire-and-forget analytics ─────────────────────────────────────────────

    /**
     * Publish a click event asynchronously. The redirect response has already
     * been dispatched to the caller before this method's subscription completes.
     *
     * <p>Errors from the analytics publisher are logged and swallowed —
     * they must never propagate back to the redirect path.
     */
    private void publishClickAsync(ShortCode code, RedirectContext context) {
        ClickEvent event = ClickEvent.minimal(code.value(), clock.instant(), context.requestId());
        analyticsPublisher.publish(event)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        null,
                        error -> log.warn("Analytics publish failed for code={}, error={}",
                                code.value(), error.getMessage())
                );
    }
}
