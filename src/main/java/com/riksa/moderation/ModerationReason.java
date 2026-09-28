package com.riksa.moderation;

/**
 * One explainable moderation signal. {@code detail} is safe for client display
 * and must describe the rule without ever copying the matched bad word.
 */
public record ModerationReason(ModerationReasonCode code, String detail) {
}
