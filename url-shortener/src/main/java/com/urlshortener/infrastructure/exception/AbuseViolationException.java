package com.urlshortener.infrastructure.exception;

public class AbuseViolationException extends RuntimeException {
    public AbuseViolationException(String message) {
        super(message);
    }
}
