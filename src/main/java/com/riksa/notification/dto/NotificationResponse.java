package com.riksa.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One inbox row. {@code actor} is null for moderator reviews. {@code postId}
 * is the community post to open; for BLOG targets open {@code targetId}.
 */
public record NotificationResponse(
        UUID id,
        String type,
        Actor actor,
        String targetType,
        UUID targetId,
        UUID postId,
        String preview,
        boolean read,
        OffsetDateTime createdAt
) {
    public record Actor(UUID id, String username, String displayName, String avatarUrl) {}
}
