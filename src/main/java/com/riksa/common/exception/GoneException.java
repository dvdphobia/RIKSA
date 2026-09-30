package com.riksa.common.exception;

/** A one-time resource (signup token, sign-in request) expired or was already used. */
public class GoneException extends RuntimeException {
    public GoneException(String message) {
        super(message);
    }
}
