package com.riksa.moderation;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The author metadata and text needed for one moderation assessment. Title and
 * body are checked together for word and general spam rules; duplicate checks
 * use the body only so callers can compare it with prior body keys.
 */
public record ModerationInput(UUID authorId, OffsetDateTime accountCreatedAt,
                              ModerationSurface surface, String title, String body) {
}
