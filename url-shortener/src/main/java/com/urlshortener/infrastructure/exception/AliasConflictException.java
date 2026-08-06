package com.urlshortener.infrastructure.exception;

public class AliasConflictException extends RuntimeException {
    public AliasConflictException(String alias) {
        super("Custom alias already exists: " + alias);
    }
}
