package com.queueforge.domain.exceptions;

public class DuplicateIdempotencyKeyException extends RuntimeException {
    public DuplicateIdempotencyKeyException(String message) {
        super(message);
    }
}
