package com.riksa.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PostModerationResponse(
        UUID id,
        String title,
        String mediaUrl,
        UUID authorId,
        UUID categoryId,
        String status,
        String hiddenReason,
        UUID hiddenBy,
        OffsetDateTime hiddenAt,
        OffsetDateTime createdAt
) {}
