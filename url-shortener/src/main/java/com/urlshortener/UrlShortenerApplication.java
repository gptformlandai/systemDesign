package com.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * URL Shortener — Spring WebFlux Application Entry Point.
 *
 * <p>Architecture: Hexagonal (Ports & Adapters)
 * <ul>
 *   <li>Domain: pure Java — no Spring/Kafka/Redis dependencies</li>
 *   <li>Ports: reactive interfaces (Mono/Flux) — define use-case and adapter contracts</li>
 *   <li>Services: use-case orchestrators — depend only on ports</li>
 *   <li>Adapters: concrete outbound (R2DBC, Redis, Kafka) and inbound (WebFlux functional routing)</li>
 * </ul>
 *
 * <p>Three-plane design (from System Design doc):
 * <ul>
 *   <li>Create/Control Plane — URL management API (POST, GET, DELETE /v1/urls/**)</li>
 *   <li>Redirect/Data Plane — public redirect (GET /{code}) — hot path, cache-first</li>
 *   <li>Background Plane — async analytics via Kafka</li>
 * </ul>
 */
@SpringBootApplication
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
