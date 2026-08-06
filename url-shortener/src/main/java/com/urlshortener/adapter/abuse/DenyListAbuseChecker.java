package com.urlshortener.adapter.abuse;

import com.urlshortener.infrastructure.exception.AbuseViolationException;
import com.urlshortener.port.outbound.AbuseChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fast synchronous deny-list abuse checker.
 *
 * <p>Checks the destination URL's registered domain against a configurable
 * deny-list. This is the synchronous gate on the create path.
 *
 * <p>For deep async scanning (malware, phishing classification), a separate
 * background event would be published post-creation — not part of this MVP.
 *
 * <p>The deny-list is loaded at startup from configuration. In production this
 * would be refreshed periodically from a threat-intelligence feed.
 */
@Slf4j
@Component
public class DenyListAbuseChecker implements AbuseChecker {

    private static final Set<String> BLOCKED_SCHEMES = Set.of("javascript", "data", "vbscript", "file");

    private final Set<String> blockedDomains;

    public DenyListAbuseChecker(
            @Value("${app.abuse.blocked-domains:malware-example.com,phishing-test.io}") String blockedDomainsConfig) {
        this.blockedDomains = Stream.of(blockedDomainsConfig.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
        log.info("AbuseChecker initialized with {} blocked domains", blockedDomains.size());
    }

    @Override
    public Mono<Void> check(String longUrl) {
        return Mono.fromRunnable(() -> {
            if (longUrl == null || longUrl.isBlank()) {
                throw new AbuseViolationException("URL must not be blank");
            }

            String lowerUrl = longUrl.toLowerCase();

            // Block unsafe schemes
            for (String scheme : BLOCKED_SCHEMES) {
                if (lowerUrl.startsWith(scheme + ":")) {
                    log.warn("Blocked unsafe scheme in URL: {}", scheme);
                    throw new AbuseViolationException("URL scheme is not allowed: " + scheme);
                }
            }

            // Check against domain deny-list
            try {
                java.net.URI uri = java.net.URI.create(longUrl);
                String host = uri.getHost();
                if (host != null) {
                    String normalizedHost = host.toLowerCase();
                    if (blockedDomains.contains(normalizedHost)) {
                        log.warn("Blocked URL with denied domain: {}", host);
                        throw new AbuseViolationException("URL destination is on the block list");
                    }
                    // Also check subdomain of blocked domains
                    for (String blocked : blockedDomains) {
                        if (normalizedHost.endsWith("." + blocked)) {
                            log.warn("Blocked URL with denied subdomain: {}", host);
                            throw new AbuseViolationException("URL destination domain is blocked");
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new AbuseViolationException("Malformed URL: " + longUrl);
            }
        });
    }
}
