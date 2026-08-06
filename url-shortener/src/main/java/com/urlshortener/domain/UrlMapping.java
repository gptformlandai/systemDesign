package com.urlshortener.domain;

import java.time.Instant;

/**
 * Core aggregate representing the mapping between a short code and its destination URL.
 *
 * <p>This is the central domain object of the URL shortener. It owns the status/expiry
 * invariant that determines whether a redirect is allowed to proceed.
 *
 * <p>Immutability: all fields are final via Java record semantics. Status updates
 * produce a new {@code UrlMapping} instance rather than mutating in place.
 *
 * <p>The {@link #isActiveAt(Instant)} method is the single gating check on the redirect
 * hot path — it must remain cheap (no I/O) and must always be called even when data
 * comes from cache.
 */
public record UrlMapping(
        ShortCode code,
        String longUrl,
        String userId,
        Instant createdAt,
        Instant expiresAt,   // null = no expiry
        UrlStatus status,
        boolean customAlias,
        long version
) {

    /**
     * Primary redirect guard: a mapping is valid for redirect only when ACTIVE
     * and its expiry (if set) has not passed at the given clock instant.
     *
     * <p>Always call this during redirect resolution even when the mapping
     * is returned from cache — the cached entry may be stale.
     */
    public boolean isActiveAt(Instant now) {
        return status == UrlStatus.ACTIVE
                && (expiresAt == null || expiresAt.isAfter(now));
    }

    /**
     * Returns a new mapping with the status changed to DISABLED.
     * Used by the URL Management service when a user deletes/disables a link.
     */
    public UrlMapping withStatus(UrlStatus newStatus) {
        return new UrlMapping(code, longUrl, userId, createdAt, expiresAt,
                newStatus, customAlias, version + 1);
    }

    /**
     * Convenience: is this mapping a custom-alias link?
     */
    public boolean isCustomAlias() {
        return customAlias;
    }
}
