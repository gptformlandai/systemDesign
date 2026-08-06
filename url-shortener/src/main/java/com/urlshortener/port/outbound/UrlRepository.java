package com.urlshortener.port.outbound;

import com.urlshortener.domain.ShortCode;
import com.urlshortener.domain.UrlMapping;
import com.urlshortener.domain.UrlStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outbound port for durable mapping persistence.
 *
 * <p>This interface hides whether the underlying implementation uses
 * PostgreSQL/R2DBC, DynamoDB, Cassandra, or an in-memory map (for tests).
 * Services depend only on this contract.
 *
 * <p>Key invariant: {@link #saveIfAbsent(UrlMapping)} must be atomic —
 * only one concurrent caller must succeed for the same code.
 */
public interface UrlRepository {

    /**
     * Persists a mapping only if the code is not already taken.
     *
     * @return {@code true} if the mapping was saved (code was unique);
     *         {@code false} if the code already existed (alias conflict)
     */
    Mono<Boolean> saveIfAbsent(UrlMapping mapping);

    /**
     * Looks up a mapping by its primary key (short code).
     * Returns empty {@code Mono} if not found.
     */
    Mono<UrlMapping> findByCode(ShortCode code);

    /**
     * Updates the status of a mapping (e.g., ACTIVE → DISABLED).
     * Uses optimistic locking via the version field.
     *
     * @return {@code true} if the update succeeded; {@code false} on version conflict
     */
    Mono<Boolean> updateStatus(ShortCode code, UrlStatus newStatus, long expectedVersion);

    /**
     * Returns all mappings owned by the given user, ordered by createdAt DESC.
     */
    Flux<UrlMapping> findByUserId(String userId, int offset, int limit);

    /**
     * Streams all mappings whose expiresAt is before {@code now}.
     * Used by the expiry cleanup background job.
     */
    Flux<UrlMapping> findExpiredBefore(java.time.Instant before);
}
