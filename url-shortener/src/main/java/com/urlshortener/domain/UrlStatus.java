package com.urlshortener.domain;

/**
 * Lifecycle states of a URL mapping.
 *
 * <p>State transitions:
 * <pre>
 *   ACTIVE -> DISABLED       (user or admin disables the link)
 *   ACTIVE -> EXPIRED        (expiry timestamp has passed)
 *   ACTIVE -> BLOCKED        (abuse/safety scan marks link unsafe)
 * </pre>
 *
 * <p>Only ACTIVE mappings are allowed to perform redirects.
 */
public enum UrlStatus {
    ACTIVE,
    DISABLED,
    EXPIRED,
    BLOCKED
}
