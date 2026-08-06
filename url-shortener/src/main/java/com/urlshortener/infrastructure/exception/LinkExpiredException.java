package com.urlshortener.infrastructure.exception;

public class LinkExpiredException extends RuntimeException {
    public LinkExpiredException(String code) {
        super("Short URL has expired: " + code);
    }
}
