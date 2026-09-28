package com.riksa.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ChatSessionDetailResponse(
        ChatSessionSummaryResponse session,
        List<Message> messages
) {
    public record Message(UUID id, String role, String content, OffsetDateTime createdAt) {}
}
