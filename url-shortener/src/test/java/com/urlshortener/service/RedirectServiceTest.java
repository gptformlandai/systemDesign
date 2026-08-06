package com.urlshortener.service;

import com.urlshortener.domain.*;
import com.urlshortener.infrastructure.exception.LinkExpiredException;
import com.urlshortener.infrastructure.exception.LinkNotFoundException;
import com.urlshortener.port.inbound.RedirectUseCase.RedirectContext;
import com.urlshortener.port.outbound.AnalyticsPublisher;
import com.urlshortener.port.outbound.CachePort;
import com.urlshortener.port.outbound.UrlRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedirectService unit tests")
class RedirectServiceTest {

    @Mock private UrlRepository urlRepository;
    @Mock private CachePort<ShortCode, UrlMapping> cache;
    @Mock private AnalyticsPublisher analyticsPublisher;

    private RedirectService service;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
    private final RedirectContext ctx = new RedirectContext("req-1", "1.2.3.4", null, "TestAgent/1.0");

    private UrlMapping activeMapping(String code) {
        return new UrlMapping(new ShortCode(code), "https://dest.com/page", "user1",
                fixedClock.instant(), null, UrlStatus.ACTIVE, false, 1L);
    }

    private UrlMapping expiredMapping(String code) {
        return new UrlMapping(new ShortCode(code), "https://dest.com/page", "user1",
                fixedClock.instant().minusSeconds(7200),
                fixedClock.instant().minusSeconds(3600), // expired 1 hour ago
                UrlStatus.ACTIVE, false, 1L);
    }

    @BeforeEach
    void setUp() {
        service = new RedirectService(urlRepository, cache, analyticsPublisher, fixedClock, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("returns longUrl on cache hit (active mapping)")
    void cacheHitSuccess() {
        UrlMapping mapping = activeMapping("abc1234");
        when(cache.get(any())).thenReturn(Mono.just(mapping));
        when(analyticsPublisher.publish(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.resolve("abc1234", ctx))
                .expectNext("https://dest.com/page")
                .verifyComplete();

        verify(urlRepository, never()).findByCode(any());
    }

    @Test
    @DisplayName("falls through to DB on cache miss")
    void cacheMissFallsToDb() {
        UrlMapping mapping = activeMapping("abc1234");
        when(cache.get(any())).thenReturn(Mono.empty());
        when(urlRepository.findByCode(any())).thenReturn(Mono.just(mapping));
        when(cache.put(any(), any(), any(Duration.class))).thenReturn(Mono.empty());
        when(analyticsPublisher.publish(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.resolve("abc1234", ctx))
                .expectNext("https://dest.com/page")
                .verifyComplete();

        verify(urlRepository).findByCode(any());
    }

    @Test
    @DisplayName("returns 404 when code not found in cache or DB")
    void notFound() {
        when(cache.get(any())).thenReturn(Mono.empty());
        when(urlRepository.findByCode(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.resolve("missing1", ctx))
                .expectError(LinkNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("returns 410 Gone for expired mapping")
    void expiredMapping() {
        UrlMapping expired = expiredMapping("abc1234");
        when(cache.get(any())).thenReturn(Mono.just(expired));
        when(cache.evict(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.resolve("abc1234", ctx))
                .expectError(LinkExpiredException.class)
                .verify();
    }

    @Test
    @DisplayName("returns 404 for disabled mapping")
    void disabledMapping() {
        UrlMapping disabled = new UrlMapping(new ShortCode("abc1234"), "https://dest.com", "u1",
                fixedClock.instant(), null, UrlStatus.DISABLED, false, 2L);
        when(cache.get(any())).thenReturn(Mono.just(disabled));

        StepVerifier.create(service.resolve("abc1234", ctx))
                .expectError(LinkNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("redirect succeeds even if analytics publisher fails")
    void analyticsFailureDoesNotBlockRedirect() {
        UrlMapping mapping = activeMapping("abc1234");
        when(cache.get(any())).thenReturn(Mono.just(mapping));
        when(analyticsPublisher.publish(any())).thenReturn(Mono.error(new RuntimeException("Kafka down")));

        // Redirect should still succeed despite analytics failure
        StepVerifier.create(service.resolve("abc1234", ctx))
                .expectNext("https://dest.com/page")
                .verifyComplete();
    }

    @Test
    @DisplayName("rejects invalid code format fast-path (no cache/DB hit)")
    void invalidCodeFormat() {
        StepVerifier.create(service.resolve("!!", ctx))
                .expectError(LinkNotFoundException.class)
                .verify();

        verifyNoInteractions(cache, urlRepository);
    }
}
