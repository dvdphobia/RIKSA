package com.riksa.common.exception;

/**
 * Thrown when a requested resource (user, gamification profile, etc.) is not found.
 * Maps to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}