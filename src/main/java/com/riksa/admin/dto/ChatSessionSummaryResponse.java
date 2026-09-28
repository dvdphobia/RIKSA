package com.riksa.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatSessionSummaryResponse(
        UUID id,
        UUID userId,
        String contextSummary,
        long messageCount,
        OffsetDateTime startedAt,
        OffsetDateTime updatedAt
) {}
