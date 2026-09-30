package com.riksa.chatAi.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        String role,
        String content,
        OffsetDateTime createdAt
) {}
