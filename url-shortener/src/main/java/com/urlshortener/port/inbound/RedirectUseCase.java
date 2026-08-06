package com.urlshortener.port.inbound;

import reactor.core.publisher.Mono;

/**
 * Inbound port for the redirect data plane.
 *
 * <p>This is the hottest path in the system. Implementations must:
 * <ul>
 *   <li>Never block</li>
 *   <li>Resolve through cache first, then metadata store</li>
 *   <li>Return a result within p99 &lt; 100 ms</li>
 *   <li>Fire analytics events asynchronously (never block redirect on analytics)</li>
 * </ul>
 */
public interface RedirectUseCase {

    /**
     * Resolves a short code to its destination URL.
     *
     * @param code      the short code extracted from the request path
     * @param context   enrichment context used for analytics (IP, referrer, etc.)
     * @return the destination URL string; errors mapped to 404/410/403 by the handler
     */
    Mono<String> resolve(String code, RedirectContext context);

    /**
     * Enrichment data attached to each redirect event.
     * Populated by the HTTP handler from request headers.
     */
    record RedirectContext(
            String requestId,
            String ipAddress,
            String referrer,
            String userAgent
    ) {}
}
