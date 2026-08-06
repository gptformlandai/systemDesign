package com.urlshortener.api;

import com.urlshortener.port.inbound.RedirectUseCase;
import com.urlshortener.port.inbound.RedirectUseCase.RedirectContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

/**
 * WebFlux functional handler for the redirect data plane.
 *
 * <p>This is the hottest handler in the system. It must:
 * <ul>
 *   <li>Extract the short code from the path</li>
 *   <li>Enrich the redirect context (requestId, IP, referrer, userAgent)</li>
 *   <li>Delegate to {@link RedirectUseCase#resolve(String, RedirectContext)}</li>
 *   <li>Return a {@code 302 Found} response with {@code Location} header</li>
 * </ul>
 *
 * <p>The redirect response is dispatched BEFORE analytics events are published.
 * No auth is required for redirect — redirects are public.
 */
@Slf4j
@Component
public class RedirectHandler {

    private final RedirectUseCase redirectUseCase;

    public RedirectHandler(RedirectUseCase redirectUseCase) {
        this.redirectUseCase = redirectUseCase;
    }

    public Mono<ServerResponse> redirect(ServerRequest request) {
        String code = request.pathVariable("code");
        RedirectContext context = buildContext(request);

        return redirectUseCase.resolve(code, context)
                .flatMap(longUrl -> ServerResponse
                        .status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, longUrl)
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                        .build())
                .doOnError(e -> log.debug("Redirect failed for code={}: {}", code, e.getMessage()));
    }

    private RedirectContext buildContext(ServerRequest request) {
        String requestId = request.headers().firstHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        String ip = request.headers().firstHeader("X-Forwarded-For");
        if (ip == null) ip = request.remoteAddress().map(a -> a.getAddress().getHostAddress()).orElse("unknown");
        String referrer = request.headers().firstHeader(HttpHeaders.REFERER);
        String userAgent = request.headers().firstHeader(HttpHeaders.USER_AGENT);

        return new RedirectContext(requestId, ip, referrer, userAgent);
    }
}
