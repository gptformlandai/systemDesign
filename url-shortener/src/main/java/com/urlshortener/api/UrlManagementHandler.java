package com.urlshortener.api;

import com.urlshortener.api.dto.CreateUrlRequest;
import com.urlshortener.api.dto.CreateUrlResponse;
import com.urlshortener.port.inbound.UrlShorteningUseCase;
import com.urlshortener.port.inbound.UrlShorteningUseCase.CreateCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * WebFlux functional handler for URL Management (control plane) operations.
 *
 * <p>Routes handled (defined in {@link UrlManagementRouter}):
 * <ul>
 *   <li>{@code POST /v1/urls} — create short URL</li>
 *   <li>{@code GET /v1/urls/{code}} — get mapping metadata</li>
 *   <li>{@code GET /v1/urls} — list user's links</li>
 *   <li>{@code DELETE /v1/urls/{code}} — disable link</li>
 * </ul>
 *
 * <p>Auth: all routes require a valid JWT. The authenticated user's principal name
 * is used as {@code userId} for all operations.
 */
@Slf4j
@Component
public class UrlManagementHandler {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final UrlShorteningUseCase urlShorteningUseCase;
    private final Validator validator;
    private final String baseUrl;

    public UrlManagementHandler(
            UrlShorteningUseCase urlShorteningUseCase,
            Validator validator,
            @Value("${app.base-url:http://localhost:8081}") String baseUrl) {
        this.urlShorteningUseCase = urlShorteningUseCase;
        this.validator = validator;
        this.baseUrl = baseUrl;
    }

    public Mono<ServerResponse> createUrl(ServerRequest request) {
        String idempotencyKey = request.headers().firstHeader(IDEMPOTENCY_KEY_HEADER);

        return authenticatedUserId(request)
                .flatMap(userId -> request.bodyToMono(CreateUrlRequest.class)
                        .flatMap(body -> validate(body).thenReturn(body))
                        .map(body -> new CreateCommand(
                                body.longUrl(),
                                userId,
                                body.customAlias(),
                                body.expiresAt()
                        ))
                        .flatMap(cmd -> urlShorteningUseCase.create(cmd, idempotencyKey))
                )
                .flatMap(mapping -> ServerResponse
                        .status(HttpStatus.CREATED)
                        .bodyValue(CreateUrlResponse.from(mapping, baseUrl)))
                .doOnError(e -> log.warn("Create URL failed: {}", e.getMessage()));
    }

    public Mono<ServerResponse> getUrl(ServerRequest request) {
        String code = request.pathVariable("code");
        return authenticatedUserId(request)
                .flatMap(userId -> urlShorteningUseCase.getByCode(code, userId))
                .flatMap(mapping -> ServerResponse.ok()
                        .bodyValue(CreateUrlResponse.from(mapping, baseUrl)));
    }

    public Mono<ServerResponse> listUrls(ServerRequest request) {
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Math.min(Integer.parseInt(request.queryParam("size").orElse("20")), 100);

        return authenticatedUserId(request)
                .flatMapMany(userId -> urlShorteningUseCase.listByUser(userId, page, size))
                .map(mapping -> CreateUrlResponse.from(mapping, baseUrl))
                .collectList()
                .flatMap(list -> ServerResponse.ok().bodyValue(list));
    }

    public Mono<ServerResponse> disableUrl(ServerRequest request) {
        String code = request.pathVariable("code");
        return authenticatedUserId(request)
                .flatMap(userId -> urlShorteningUseCase.disable(code, userId))
                .flatMap(mapping -> ServerResponse.ok()
                        .bodyValue(CreateUrlResponse.from(mapping, baseUrl)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Mono<String> authenticatedUserId(ServerRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName());
    }

    private Mono<Void> validate(CreateUrlRequest body) {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(body, "createUrlRequest");
        validator.validate(body, errors);
        if (errors.hasErrors()) {
            String message = errors.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Validation failed");
            return Mono.error(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, message));
        }
        return Mono.empty();
    }
}
