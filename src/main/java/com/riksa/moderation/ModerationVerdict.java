package com.riksa.moderation;

/**
 * Outcome of the explainable, rule-based content check. {@link #ALLOW} publishes
 * immediately, {@link #REVIEW} marks content for a later policy decision, and
 * {@link #BLOCK} rejects content because it contains a severe term or reaches
 * the configured block score.
 */
public enum ModerationVerdict {
    ALLOW,
    REVIEW,
    BLOCK
}
