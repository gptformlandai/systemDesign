package com.urlshortener.service;

import com.urlshortener.domain.*;
import com.urlshortener.infrastructure.exception.*;
import com.urlshortener.port.inbound.UrlShorteningUseCase;
import com.urlshortener.port.outbound.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

/**
 * Control plane service orchestrating URL creation, retrieval, and lifecycle management.
 *
 * <p>Responsibilities (follows the design doc's create path):
 * <pre>
 *   validate URL → check abuse → idempotency check → generate/reserve code
 *   → save mapping → evict stale cache entry → return mapping
 * </pre>
 *
 * <p>This class depends only on port interfaces — no HTTP, Redis, or Kafka imports.
 * Framework wiring is provided via constructor injection.
 */
@Slf4j
@Service
public class UrlShorteningService implements UrlShorteningUseCase {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final int MAX_CODE_COLLISION_RETRIES = 5;

    private final UrlRepository urlRepository;
    private final CodeGenerator codeGenerator;
    private final CachePort<ShortCode, UrlMapping> cache;
    private final AbuseChecker abuseChecker;
    private final IdempotencyStore idempotencyStore;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public UrlShorteningService(
            UrlRepository urlRepository,
            CodeGenerator codeGenerator,
            CachePort<ShortCode, UrlMapping> cache,
            AbuseChecker abuseChecker,
            IdempotencyStore idempotencyStore,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.urlRepository = urlRepository;
        this.codeGenerator = codeGenerator;
        this.cache = cache;
        this.abuseChecker = abuseChecker;
        this.idempotencyStore = idempotencyStore;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<UrlMapping> create(CreateCommand command, String idempotencyKey) {
        return validateRequest(command)
                .then(Mono.defer(() -> abuseChecker.check(command.longUrl())))
                .then(Mono.defer(() -> resolveIdempotent(command, idempotencyKey)))
                .doOnSuccess(m -> meterRegistry.counter("url.create.success").increment())
                .doOnError(e -> meterRegistry.counter("url.create.error",
                        "reason", e.getClass().getSimpleName()).increment());
    }

    @Override
    public Mono<UrlMapping> getByCode(String code, String requestingUserId) {
        ShortCode shortCode;
        try {
            shortCode = new ShortCode(code);
        } catch (IllegalArgumentException e) {
            return Mono.error(new LinkNotFoundException(code));
        }
        return urlRepository.findByCode(shortCode)
                .filter(m -> m.userId().equals(requestingUserId))
                .switchIfEmpty(Mono.error(new LinkNotFoundException(code)));
    }

    @Override
    public Flux<UrlMapping> listByUser(String userId, int page, int size) {
        int offset = page * size;
        return urlRepository.findByUserId(userId, offset, size);
    }

    @Override
    public Mono<UrlMapping> disable(String code, String requestingUserId) {
        ShortCode shortCode;
        try {
            shortCode = new ShortCode(code);
        } catch (IllegalArgumentException e) {
            return Mono.error(new LinkNotFoundException(code));
        }
        return urlRepository.findByCode(shortCode)
                .switchIfEmpty(Mono.error(new LinkNotFoundException(code)))
                .flatMap(mapping -> {
                    if (!mapping.userId().equals(requestingUserId)) {
                        return Mono.error(new LinkNotFoundException(code)); // 404, not 403 — avoid info leak
                    }
                    return urlRepository.updateStatus(shortCode, UrlStatus.DISABLED, mapping.version())
                            .flatMap(updated -> {
                                if (!updated) {
                                    return Mono.error(new IllegalStateException("Concurrent modification, please retry"));
                                }
                                return cache.evict(shortCode)
                                        .thenReturn(mapping.withStatus(UrlStatus.DISABLED));
                            });
                });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Mono<Void> validateRequest(CreateCommand command) {
        if (command.longUrl() == null || command.longUrl().isBlank()) {
            return Mono.error(new InvalidUrlException("URL must not be blank"));
        }
        try {
            java.net.URI uri = java.net.URI.create(command.longUrl());
            if (!ALLOWED_SCHEMES.contains(uri.getScheme())) {
                return Mono.error(new InvalidUrlException(
                        "Only http and https URLs are supported, got scheme: " + uri.getScheme()));
            }
        } catch (IllegalArgumentException e) {
            return Mono.error(new InvalidUrlException("Malformed URL: " + command.longUrl()));
        }
        if (command.expiresAt() != null && !command.expiresAt().isAfter(clock.instant())) {
            return Mono.error(new InvalidUrlException("Expiry must be a future timestamp"));
        }
        return Mono.empty();
    }

    private Mono<UrlMapping> resolveIdempotent(CreateCommand command, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank() && command.userId() != null) {
            return idempotencyStore.get(idempotencyKey, command.userId())
                    .flatMap(record -> urlRepository.findByCode(record.resolvedCode()))
                    .switchIfEmpty(Mono.defer(() -> createAndStore(command, idempotencyKey)));
        }
        return createAndStore(command, null);
    }

    private Mono<UrlMapping> createAndStore(CreateCommand command, String idempotencyKey) {
        return (command.customAlias() != null && !command.customAlias().isBlank())
                ? reserveCustomAlias(command)
                : generateAndReserve(command, MAX_CODE_COLLISION_RETRIES)
                        .flatMap(mapping -> storeIdempotencyRecord(mapping, command.userId(), idempotencyKey));
    }

    private Mono<UrlMapping> reserveCustomAlias(CreateCommand command) {
        ShortCode alias;
        try {
            alias = ShortCode.ofCustomAlias(command.customAlias());
        } catch (IllegalArgumentException e) {
            return Mono.error(new InvalidUrlException("Invalid custom alias: " + command.customAlias()));
        }
        UrlMapping mapping = buildMapping(alias, command, true);
        return urlRepository.saveIfAbsent(mapping)
                .flatMap(saved -> saved
                        ? Mono.just(mapping)
                        : Mono.error(new AliasConflictException(command.customAlias())));
    }

    private Mono<UrlMapping> generateAndReserve(CreateCommand command, int attemptsLeft) {
        if (attemptsLeft <= 0) {
            return Mono.error(new IllegalStateException("Unable to reserve unique short code after retries"));
        }
        ShortCode code = ShortCode.ofGenerated(codeGenerator.nextCode());
        UrlMapping mapping = buildMapping(code, command, false);
        return urlRepository.saveIfAbsent(mapping)
                .flatMap(saved -> saved
                        ? Mono.just(mapping)
                        : generateAndReserve(command, attemptsLeft - 1));
    }

    private UrlMapping buildMapping(ShortCode code, CreateCommand command, boolean customAlias) {
        return new UrlMapping(
                code,
                command.longUrl(),
                command.userId(),
                clock.instant(),
                command.expiresAt(),
                UrlStatus.ACTIVE,
                customAlias,
                1L
        );
    }

    private Mono<UrlMapping> storeIdempotencyRecord(UrlMapping mapping, String userId, String idempotencyKey) {
        if (idempotencyKey == null || userId == null) {
            return Mono.just(mapping);
        }
        IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, userId, mapping.code(), Instant.now(clock));
        return idempotencyStore.save(record).thenReturn(mapping);
    }

    // ── Inner port for idempotency (Redis-backed in production) ──────────────

    /**
     * Secondary outbound port for idempotency record storage.
     * Extracted as a separate port to keep {@link UrlRepository} focused on URL mappings.
     */
    public interface IdempotencyStore {
        Mono<IdempotencyRecord> get(String key, String userId);
        Mono<Void> save(IdempotencyRecord record);
    }
}
