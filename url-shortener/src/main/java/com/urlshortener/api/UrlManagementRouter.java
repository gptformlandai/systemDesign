package com.urlshortener.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * WebFlux functional router for the URL Management (control plane) API.
 *
 * <p>Route table:
 * <pre>
 *   POST   /v1/urls          — create short URL (auth required)
 *   GET    /v1/urls          — list user's URLs (auth required)
 *   GET    /v1/urls/{code}   — get URL metadata (auth required)
 *   DELETE /v1/urls/{code}   — disable URL (auth required)
 * </pre>
 *
 * <p>Uses functional routing (no {@code @Controller} annotations) — the idiomatic
 * WebFlux pattern for production services that need explicit route control.
 */
@Configuration
public class UrlManagementRouter {

    @Bean
    public RouterFunction<ServerResponse> urlManagementRoutes(UrlManagementHandler handler) {
        return RouterFunctions.route()
                .POST("/v1/urls", accept(MediaType.APPLICATION_JSON), handler::createUrl)
                .GET("/v1/urls", handler::listUrls)
                .GET("/v1/urls/{code}", handler::getUrl)
                .DELETE("/v1/urls/{code}", handler::disableUrl)
                .build();
    }
}
