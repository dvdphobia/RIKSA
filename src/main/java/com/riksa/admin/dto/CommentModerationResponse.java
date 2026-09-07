package com.riksa.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommentModerationResponse(
        UUID id,
        String content,
        String mediaUrl,
        UUID postId,
        UUID authorId,
        String status,
        String hiddenReason,
        UUID hiddenBy,
        OffsetDateTime hiddenAt,
        OffsetDateTime createdAt
) {}
