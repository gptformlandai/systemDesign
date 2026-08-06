package com.urlshortener.port.outbound;

import reactor.core.publisher.Mono;

/**
 * Outbound port for URL safety and abuse checking.
 *
 * <p>Two levels of checking are expected:
 * <ol>
 *   <li><b>Synchronous</b> — fast deny-list check during URL creation.
 *       Must complete within a short timeout (&lt; 200 ms) or the creation
 *       is rejected with a policy error.</li>
 *   <li><b>Asynchronous</b> — deep malware/phishing scan triggered after
 *       creation. If the scan later marks a URL unsafe, a separate event
 *       triggers {@link UrlRepository#updateStatus} to BLOCKED and publishes
 *       a cache-invalidation event.</li>
 * </ol>
 *
 * <p>This port is only invoked on the <em>create path</em>, never on the redirect path.
 */
public interface AbuseChecker {

    /**
     * Performs a fast, synchronous deny-list check.
     *
     * @param longUrl the destination URL to validate
     * @return empty {@code Mono} if the URL is allowed; error signal if blocked
     * @throws com.urlshortener.infrastructure.exception.AbuseViolationException if blocked
     */
    Mono<Void> check(String longUrl);
}
