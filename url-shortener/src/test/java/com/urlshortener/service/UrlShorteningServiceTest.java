package com.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.urlshortener.domain.*;
import com.urlshortener.infrastructure.exception.AliasConflictException;
import com.urlshortener.infrastructure.exception.InvalidUrlException;
import com.urlshortener.port.inbound.UrlShorteningUseCase.CreateCommand;
import com.urlshortener.port.outbound.AbuseChecker;
import com.urlshortener.port.outbound.CachePort;
import com.urlshortener.port.outbound.CodeGenerator;
import com.urlshortener.port.outbound.UrlRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlShorteningService unit tests")
class UrlShorteningServiceTest {

    @Mock private UrlRepository urlRepository;
    @Mock private CodeGenerator codeGenerator;
    @Mock private CachePort<ShortCode, UrlMapping> cache;
    @Mock private AbuseChecker abuseChecker;
    @Mock private UrlShorteningService.IdempotencyStore idempotencyStore;

    private UrlShorteningService service;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        service = new UrlShorteningService(
                urlRepository, codeGenerator, cache, abuseChecker, idempotencyStore,
                fixedClock, new SimpleMeterRegistry());
    }

    @Nested
    @DisplayName("create() — generated code")
    class GeneratedCodeTests {

        @Test
        @DisplayName("creates mapping with generated code on success")
        void createGeneratedSuccess() {
            when(abuseChecker.check(anyString())).thenReturn(Mono.empty());
            when(codeGenerator.nextCode()).thenReturn("abc1234");
            when(urlRepository.saveIfAbsent(any())).thenReturn(Mono.just(true));

            CreateCommand cmd = new CreateCommand("https://example.com", "user1", null, null);

            StepVerifier.create(service.create(cmd, null))
                    .assertNext(mapping -> {
                        assertThat(mapping.code().value()).isEqualTo("abc1234");
                        assertThat(mapping.status()).isEqualTo(UrlStatus.ACTIVE);
                        assertThat(mapping.userId()).isEqualTo("user1");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("retries code generation on collision")
        void retriesOnCollision() {
            when(abuseChecker.check(anyString())).thenReturn(Mono.empty());
            when(codeGenerator.nextCode()).thenReturn("collide1", "collide1", "unique12");

            when(urlRepository.saveIfAbsent(argThat(m -> m != null && m.code() != null && "collide1".equals(m.code().value()))))
                    .thenReturn(Mono.just(false));
            when(urlRepository.saveIfAbsent(argThat(m -> m != null && m.code() != null && "unique12".equals(m.code().value()))))
                    .thenReturn(Mono.just(true));
            CreateCommand cmd = new CreateCommand("https://example.com", "user1", null, null);

            StepVerifier.create(service.create(cmd, null))
                    .assertNext(m -> assertThat(m.code().value()).isEqualTo("unique12"))
                    .verifyComplete();

            verify(urlRepository, times(3)).saveIfAbsent(any());
        }

        @Test
        @DisplayName("rejects invalid URL scheme")
        void rejectsInvalidScheme() {
            CreateCommand cmd = new CreateCommand("javascript:alert(1)", "user1", null, null);

            StepVerifier.create(service.create(cmd, null))
                    .expectError(InvalidUrlException.class)
                    .verify();
        }

        @Test
        @DisplayName("rejects blank URL")
        void rejectsBlankUrl() {
            CreateCommand cmd = new CreateCommand("", "user1", null, null);

            StepVerifier.create(service.create(cmd, null))
                    .expectError(InvalidUrlException.class)
                    .verify();
        }

        @Test
        @DisplayName("rejects past expiry timestamp")
        void rejectsPastExpiry() {
            Instant pastTime = fixedClock.instant().minusSeconds(3600);
            CreateCommand cmd = new CreateCommand("https://example.com", "user1", null, pastTime);

            StepVerifier.create(service.create(cmd, null))
                    .expectError(InvalidUrlException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("create() — custom alias")
    class CustomAliasTests {

        @Test
        @DisplayName("reserves custom alias atomically")
        void reservesAlias() {
            when(abuseChecker.check(anyString())).thenReturn(Mono.empty());
            when(urlRepository.saveIfAbsent(any())).thenReturn(Mono.just(true));

            CreateCommand cmd = new CreateCommand("https://example.com", "user1", "my-alias", null);

            StepVerifier.create(service.create(cmd, null))
                    .assertNext(m -> assertThat(m.code().value()).isEqualTo("my-alias"))
                    .verifyComplete();

            verify(codeGenerator, never()).nextCode();
        }

        @Test
        @DisplayName("returns AliasConflictException when alias is taken")
        void conflictOnTakenAlias() {
            when(abuseChecker.check(anyString())).thenReturn(Mono.empty());
            when(urlRepository.saveIfAbsent(any())).thenReturn(Mono.just(false));

            CreateCommand cmd = new CreateCommand("https://example.com", "user1", "taken-alias", null);

            StepVerifier.create(service.create(cmd, null))
                    .expectError(AliasConflictException.class)
                    .verify();
        }
    }
}
