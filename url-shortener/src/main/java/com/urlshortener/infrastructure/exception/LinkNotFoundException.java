package com.urlshortener.infrastructure.exception;

public class LinkNotFoundException extends RuntimeException {
    public LinkNotFoundException(String code) {
        super("Short URL not found: " + code);
    }
}
