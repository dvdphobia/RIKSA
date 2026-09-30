package com.riksa.moderation;

/**
 * Stable machine-readable reason codes returned by the moderation rules.
 * Clients should localize these codes instead of displaying internal details.
 */
public enum ModerationReasonCode {
    PROFANITY,
    SPAM_LINKS,
    UNKNOWN_LINK,
    DUPLICATE,
    SHOUTING,
    REPEATED_CHARS,
    LINK_ONLY,
    NEW_ACCOUNT,
    RATE_LIMITED
}
