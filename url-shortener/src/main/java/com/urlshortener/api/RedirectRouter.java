package com.urlshortener.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Set;

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

    // Top-level path segments that must NOT be treated as short codes.
    private static final Set<String> RESERVED_SEGMENTS = Set.of(
            "swagger-ui", "v3", "webjars", "actuator", "v1"
    );

    private static RequestPredicate notReserved() {
        return request -> {
            String[] parts = request.path().split("/", 3);
            // parts[0] is always "" (leading slash), parts[1] is the first segment
            return parts.length > 1 && !RESERVED_SEGMENTS.contains(parts[1]);
        };
    }

    @RouterOperations(@RouterOperation(
        path = "/{code}", method = RequestMethod.GET,
        beanClass = RedirectHandler.class, beanMethod = "redirect",
        operation = @Operation(
            operationId = "redirect", summary = "Redirect to original URL", tags = {"Redirect"},
            parameters = @Parameter(name = "code", in = ParameterIn.PATH, required = true,
                description = "Short URL code", schema = @Schema(type = "string")),
            responses = {
                @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
                @ApiResponse(responseCode = "404", description = "Short URL not found")
            }
        )
    ))
    @Bean
    public RouterFunction<ServerResponse> redirectRoutes(RedirectHandler handler) {
        return RouterFunctions.route()
                .GET("/{code}", notReserved(), handler::redirect)
                .build();
    }
}
