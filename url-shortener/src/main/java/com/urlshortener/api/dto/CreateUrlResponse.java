package com.urlshortener.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.urlshortener.domain.UrlMapping;

import java.time.Instant;

/**
 * HTTP response body for URL creation and retrieval endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateUrlResponse(
        String code,
        String shortUrl,
        String longUrl,
        Instant expiresAt,
        String status,
        Instant createdAt
) {
    public static CreateUrlResponse from(UrlMapping mapping, String baseUrl) {
        return new CreateUrlResponse(
                mapping.code().value(),
                baseUrl + "/" + mapping.code().value(),
                mapping.longUrl(),
                mapping.expiresAt(),
                mapping.status().name(),
                mapping.createdAt()
        );
    }
}
