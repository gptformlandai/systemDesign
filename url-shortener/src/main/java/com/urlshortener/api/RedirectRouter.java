package com.urlshortener.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * WebFlux functional router for the redirect (data plane) endpoint.
 *
 * <p>Route table:
 * <pre>
 *   GET /{code}  — resolve short code and redirect (public, no auth)
 * </pre>
 *
 * <p>This route is registered separately from the management API so that:
 * <ul>
 *   <li>Security configuration can exempt it from JWT auth</li>
 *   <li>Rate limiting and CDN headers can be applied independently</li>
 *   <li>It can eventually move to a dedicated port in production</li>
 * </ul>
 */
@Configuration
public class RedirectRouter {

    @Bean
    public RouterFunction<ServerResponse> redirectRoutes(RedirectHandler handler) {
        return RouterFunctions.route()
                .GET("/{code}", handler::redirect)
                .build();
    }
}
