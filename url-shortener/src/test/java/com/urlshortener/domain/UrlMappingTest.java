package com.urlshortener.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the {@link UrlMapping} domain aggregate.
 *
 * <p>Pure domain tests — no Spring context, no mocks.
 */
@DisplayName("UrlMapping domain tests")
class UrlMappingTest {

    private static final ShortCode CODE = new ShortCode("abc1234");
    private static final String LONG_URL = "https://example.com/products/123";
    private static final String USER_ID = "user-42";
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    private UrlMapping active(Instant expiresAt) {
        return new UrlMapping(CODE, LONG_URL, USER_ID, CREATED, expiresAt, UrlStatus.ACTIVE, false, 1L);
    }

    private UrlMapping withStatus(UrlStatus status) {
        return new UrlMapping(CODE, LONG_URL, USER_ID, CREATED, null, status, false, 1L);
    }

    @Nested
    @DisplayName("isActiveAt()")
    class IsActiveAt {

        @Test
        @DisplayName("returns true when ACTIVE and no expiry")
        void activeNoExpiry() {
            UrlMapping mapping = active(null);
            assertThat(mapping.isActiveAt(Instant.now())).isTrue();
        }

        @Test
        @DisplayName("returns true when ACTIVE and expiry is in the future")
        void activeNotExpired() {
            UrlMapping mapping = active(Instant.now().plusSeconds(3600));
            assertThat(mapping.isActiveAt(Instant.now())).isTrue();
        }

        @Test
        @DisplayName("returns false when ACTIVE but expiry has passed")
        void activeExpired() {
            UrlMapping mapping = active(Instant.now().minusSeconds(1));
            assertThat(mapping.isActiveAt(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("returns false when DISABLED regardless of expiry")
        void disabled() {
            UrlMapping mapping = withStatus(UrlStatus.DISABLED);
            assertThat(mapping.isActiveAt(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("returns false when BLOCKED")
        void blocked() {
            UrlMapping mapping = withStatus(UrlStatus.BLOCKED);
            assertThat(mapping.isActiveAt(Instant.now())).isFalse();
        }
    }

    @Nested
    @DisplayName("withStatus()")
    class WithStatus {

        @Test
        @DisplayName("produces a new instance with the updated status")
        void producesNewInstance() {
            UrlMapping original = withStatus(UrlStatus.ACTIVE);
            UrlMapping disabled = original.withStatus(UrlStatus.DISABLED);

            assertThat(disabled.status()).isEqualTo(UrlStatus.DISABLED);
            assertThat(disabled.version()).isEqualTo(original.version() + 1);
            assertThat(original.status()).isEqualTo(UrlStatus.ACTIVE); // immutable
        }
    }
}
