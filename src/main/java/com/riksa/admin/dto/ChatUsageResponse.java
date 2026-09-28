package com.riksa.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ChatUsageResponse(
        int days,
        long totalMessages,
        long activeUsers,
        int dailyLimit,
        boolean enabled,
        List<TopUser> topUsers
) {
    public record TopUser(
            UUID userId,
            String username,
            String displayName,
            long messages,
            long sessions,
            OffsetDateTime lastMessageAt
    ) {}
}
