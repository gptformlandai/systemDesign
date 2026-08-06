package com.urlshortener.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * HTTP request body for {@code POST /v1/urls}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateUrlRequest(

        @NotBlank(message = "longUrl must not be blank")
        String longUrl,

        @Size(min = 3, max = 32, message = "customAlias must be between 3 and 32 characters")
        String customAlias,   // null = auto-generate

        Instant expiresAt     // null = no expiry
) {}
