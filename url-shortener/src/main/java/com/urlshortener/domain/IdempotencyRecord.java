package com.urlshortener.domain;

import java.time.Instant;

/**
 * Idempotency guard record for URL creation.
 *
 * <p>When a client provides an {@code Idempotency-Key} header, the result of the
 * first successful create is stored in Redis under this record. Subsequent retries
 * with the same key (same userId) return the same {@link ShortCode} without creating
 * a duplicate mapping.
 *
 * <p>TTL for idempotency records is 24 hours (configured in application.yml).
 */
public record IdempotencyRecord(
        String idempotencyKey,
        String userId,
        ShortCode resolvedCode,
        Instant createdAt
) {}
