package com.riksa.common.exception;

/**
 * Thrown when a cursor parameter cannot be decoded or is malformed.
 * Maps to HTTP 400.
 */
public class InvalidCursorException extends RuntimeException {
    public InvalidCursorException(String message) {
        super(message);
    }

    public InvalidCursorException(String message, Throwable cause) {
        super(message, cause);
    }
}