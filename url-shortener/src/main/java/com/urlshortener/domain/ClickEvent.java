package com.urlshortener.domain;

import java.time.Instant;

/**
 * Analytics event produced for every successful redirect.
 *
 * <p>This record is emitted asynchronously (fire-and-forget) from the Redirect Service
 * and published to a Kafka topic. It must never block the redirect response.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>{@code ipHash} — hashed IP to avoid PII in the event stream</li>
 *   <li>{@code requestId} — enables deduplication if exactly-once semantics are needed later</li>
 *   <li>All fields nullable-safe — analytics must degrade gracefully, not throw</li>
 * </ul>
 */
public record ClickEvent(
        String code,
        Instant clickedAt,
        String ipHash,         // hashed, not raw IP
        String country,        // GeoIP-resolved, may be null
        String referrer,       // null if direct navigation
        String userAgentFamily,// browser family, e.g. "Chrome"
        String requestId       // trace ID for deduplication
) {

    /**
     * Minimal factory for tests and the redirect hot path where full enrichment
     * is not yet available.
     */
    public static ClickEvent minimal(String code, Instant clickedAt, String requestId) {
        return new ClickEvent(code, clickedAt, null, null, null, null, requestId);
    }
}
