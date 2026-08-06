package com.urlshortener.infrastructure.exception;

import com.urlshortener.api.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Global reactive exception handler.
 *
 * <p>Maps domain exceptions to structured HTTP error responses.
 * Ordered at -2 to run before Spring Boot's default error handler.
 *
 * <p>Error response format follows RFC 7807 (Problem Details for HTTP APIs).
 */
@Slf4j
@Order(-2)
@Component
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = resolveStatus(ex);
        String path = exchange.getRequest().getPath().value();
        ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), ex.getMessage(), path);

        if (status.is5xxServerError()) {
            log.error("Unexpected server error at path={}: {}", path, ex.getMessage(), ex);
        } else {
            log.debug("Client error {} at path={}: {}", status.value(), path, ex.getMessage());
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"error\":\"Internal error\"}").getBytes();
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private HttpStatus resolveStatus(Throwable ex) {
        return switch (ex) {
            case LinkNotFoundException ignored         -> HttpStatus.NOT_FOUND;
            case LinkExpiredException ignored         -> HttpStatus.GONE;
            case AliasConflictException ignored       -> HttpStatus.CONFLICT;
            case InvalidUrlException ignored          -> HttpStatus.BAD_REQUEST;
            case AbuseViolationException ignored      -> HttpStatus.FORBIDDEN;
            case ResponseStatusException rse          -> HttpStatus.resolve(rse.getStatusCode().value()) != null
                    ? HttpStatus.resolve(rse.getStatusCode().value())
                    : HttpStatus.INTERNAL_SERVER_ERROR;
            case IllegalArgumentException ignored     -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
