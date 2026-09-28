package com.riksa.moderation;

import java.util.List;

/**
 * Signals that a moderation result is BLOCK. The exception carries stable
 * reason codes for REST and GraphQL error mapping; it never carries matched
 * word-list terms.
 */
public class ContentBlockedException extends RuntimeException {

    private final List<ModerationReason> reasons;

    public ContentBlockedException(List<ModerationReason> reasons) {
        super("Content was blocked by moderation.");
        this.reasons = List.copyOf(reasons);
    }

    public List<ModerationReason> reasons() {
        return reasons;
    }
}
