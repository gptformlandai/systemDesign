package com.urlshortener.api;

import com.urlshortener.api.dto.CreateUrlRequest;
import com.urlshortener.api.dto.CreateUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
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
 */
@Configuration
public class UrlManagementRouter {

    @RouterOperations({
        @RouterOperation(
            path = "/v1/urls", method = RequestMethod.POST,
            beanClass = UrlManagementHandler.class, beanMethod = "createUrl",
            operation = @Operation(
                operationId = "createUrl", summary = "Create a short URL", tags = {"URLs"},
                security = @SecurityRequirement(name = "bearerAuth"),
                requestBody = @RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = CreateUrlRequest.class))),
                responses = @ApiResponse(responseCode = "201", description = "Short URL created",
                    content = @Content(schema = @Schema(implementation = CreateUrlResponse.class)))
            )
        ),
        @RouterOperation(
            path = "/v1/urls", method = RequestMethod.GET,
            beanClass = UrlManagementHandler.class, beanMethod = "listUrls",
            operation = @Operation(
                operationId = "listUrls", summary = "List user's short URLs", tags = {"URLs"},
                security = @SecurityRequirement(name = "bearerAuth"),
                parameters = {
                    @Parameter(name = "page", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "0")),
                    @Parameter(name = "size", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "20"))
                },
                responses = @ApiResponse(responseCode = "200", description = "Paginated URL list",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CreateUrlResponse.class))))
            )
        ),
        @RouterOperation(
            path = "/v1/urls/{code}", method = RequestMethod.GET,
            beanClass = UrlManagementHandler.class, beanMethod = "getUrl",
            operation = @Operation(
                operationId = "getUrl", summary = "Get URL metadata by code", tags = {"URLs"},
                security = @SecurityRequirement(name = "bearerAuth"),
                parameters = @Parameter(name = "code", in = ParameterIn.PATH, required = true,
                    schema = @Schema(type = "string")),
                responses = @ApiResponse(responseCode = "200", description = "URL metadata",
                    content = @Content(schema = @Schema(implementation = CreateUrlResponse.class)))
            )
        ),
        @RouterOperation(
            path = "/v1/urls/{code}", method = RequestMethod.DELETE,
            beanClass = UrlManagementHandler.class, beanMethod = "disableUrl",
            operation = @Operation(
                operationId = "disableUrl", summary = "Disable a short URL", tags = {"URLs"},
                security = @SecurityRequirement(name = "bearerAuth"),
                parameters = @Parameter(name = "code", in = ParameterIn.PATH, required = true,
                    schema = @Schema(type = "string")),
                responses = @ApiResponse(responseCode = "200", description = "URL disabled",
                    content = @Content(schema = @Schema(implementation = CreateUrlResponse.class)))
            )
        )
    })
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
