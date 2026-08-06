package com.urlshortener.port.outbound;

import com.urlshortener.domain.ClickEvent;
import reactor.core.publisher.Mono;

/**
 * Outbound port for click analytics event publishing.
 *
 * <p>The Redirect Service calls this after resolving a valid mapping.
 * Implementation must be non-blocking; the redirect response is sent to the
 * browser before this call completes (fire-and-forget pattern).
 *
 * <p>Failure contract:
 * <ul>
 *   <li>If the analytics pipeline is unavailable, implementation must NOT propagate
 *       the error back to the redirect path — it should log, buffer, or drop the event.</li>
 *   <li>A Resilience4j circuit breaker wraps this port at the service layer.</li>
 * </ul>
 */
public interface AnalyticsPublisher {

    /**
     * Publishes a click event asynchronously.
     *
     * @param event the enriched click event
     * @return a {@code Mono<Void>} that completes when the event is enqueued
     *         (not necessarily when it reaches the OLAP store)
     */
    Mono<Void> publish(ClickEvent event);
}
