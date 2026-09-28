package com.riksa.moderation;

/**
 * Signals that a posting rate rule rejected the request. REST and GraphQL
 * clients receive the stable {@code RATE_LIMITED} code and a short English
 * message so their apps can show a localized response.
 */
public class RateLimitedException extends RuntimeException {

    public RateLimitedException() {
        super("Please wait before posting again.");
    }
}
