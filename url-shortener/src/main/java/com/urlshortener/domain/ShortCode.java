package com.urlshortener.domain;

import java.util.regex.Pattern;

/**
 * Value object representing a validated short code.
 *
 * <p>Invariants:
 * <ul>
 *   <li>Characters: {@code [A-Za-z0-9_-]}</li>
 *   <li>Length: 3 – 32 characters</li>
 *   <li>Immutable after construction</li>
 * </ul>
 *
 * <p>This object has no framework dependencies and can be used safely in
 * domain logic, adapters, and tests without any mocking.
 */
public record ShortCode(String value) {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");
    private static final int GENERATED_CODE_MIN_LENGTH = 7;

    public ShortCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Short code must not be blank");
        }
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Short code must match [A-Za-z0-9_-]{3,32}, got: " + value);
        }
    }

    /**
     * Factory method for generated codes. Enforces minimum 7-character length
     * for the generated Base62 namespace, matching the design doc's capacity
     * recommendation of ~3.5 trillion unique codes.
     */
    public static ShortCode ofGenerated(String value) {
        if (value == null || value.length() < GENERATED_CODE_MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "Generated code must be at least " + GENERATED_CODE_MIN_LENGTH + " characters");
        }
        return new ShortCode(value);
    }

    /**
     * Factory method for user-supplied custom aliases. Subject to the same
     * character constraints but may be shorter (minimum 3 chars).
     */
    public static ShortCode ofCustomAlias(String value) {
        return new ShortCode(value); // validation runs in compact constructor
    }

    @Override
    public String toString() {
        return value;
    }
}
