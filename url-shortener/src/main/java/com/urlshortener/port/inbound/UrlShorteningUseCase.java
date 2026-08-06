package com.urlshortener.port.inbound;

import com.urlshortener.domain.UrlMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Inbound port for URL creation, retrieval, and lifecycle management.
 *
 * <p>This interface defines the use-case boundary of the control plane.
 * HTTP handlers and gRPC adapters depend on this interface — never on
 * the concrete service implementation.
 */
public interface UrlShorteningUseCase {

    /**
     * Creates a new short URL mapping.
     *
     * @param command      the creation command (validated before reaching service)
     * @param idempotencyKey nullable; if provided, duplicate retries return the same mapping
     * @return the persisted mapping
     */
    Mono<UrlMapping> create(CreateCommand command, String idempotencyKey);

    /**
     * Retrieves a mapping by short code. Returns empty if not found.
     */
    Mono<UrlMapping> getByCode(String code, String requestingUserId);

    /**
     * Lists all mappings owned by a user, ordered by creation time descending.
     */
    Flux<UrlMapping> listByUser(String userId, int page, int size);

    /**
     * Disables a mapping. Publishes a cache-invalidation event.
     *
     * @return the updated (disabled) mapping
     */
    Mono<UrlMapping> disable(String code, String requestingUserId);

    /**
     * Immutable command for URL creation.
     */
    record CreateCommand(
            String longUrl,
            String userId,
            String customAlias,   // null = generate code
            Instant expiresAt     // null = no expiry
    ) {}
}
