package com.riksa.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record XpAuditRow(
        UUID userId,
        String username,
        String displayName,
        long xpGained,
        long transactionCount,
        long lessonCompletions,
        long totalXp,
        OffsetDateTime firstAt,
        OffsetDateTime lastAt,
        boolean suspended,
        boolean leaderboardExcluded) {
}
