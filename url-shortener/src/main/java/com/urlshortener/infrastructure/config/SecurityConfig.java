package com.urlshortener.infrastructure.config;

import com.urlshortener.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security configuration for WebFlux.
 *
 * <p>Security policy:
 * <ul>
 *   <li>{@code GET /{code}} — public (no auth required; redirects are public)</li>
 *   <li>{@code /actuator/health, /actuator/prometheus} — public</li>
 *   <li>All {@code /v1/**} endpoints — JWT required</li>
 * </ul>
 *
 * <p>CSRF is disabled (stateless API, JWT-based auth).
 * HTTP Basic is disabled.
 * Form login is disabled.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Public: redirect data plane
                        .pathMatchers(HttpMethod.GET, "/{code:[A-Za-z0-9_-]{3,32}}").permitAll()
                        // Public: health + metrics
                        .pathMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        // Public: Swagger & OpenAPI docs
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/swagger-ui/**").permitAll()
                        // Protected: all management API routes
                        .pathMatchers("/v1/**").authenticated()
                        // Everything else denied
                        .anyExchange().denyAll()
                )
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
