package com.riksa.admin.dto;

import com.riksa.report.enums.ReportReason;
import com.riksa.report.enums.ReportStatus;
import com.riksa.report.enums.ReportTargetType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UserOverviewResponse(
        AdminUserResponse user,
        int failedLoginAttempts,
        boolean leaderboardExcluded,
        GamificationOverview gamification,
        List<AchievementOverview> achievements,
        long lessonsCompleted,
        List<RecentCompletion> recentCompletions,
        long reportsFiled,
        List<ReportOverview> reportsAgainst
) {
    public record GamificationOverview(
            long totalXp,
            long currentStreak,
            long highestStreak,
            OffsetDateTime lastCompletedAt,
            String tier,
            String tierIconUrl
    ) {}

    public record AchievementOverview(
            UUID id,
            String name,
            String iconUrl,
            String rarity,
            int xpReward,
            boolean active,
            boolean earned,
            OffsetDateTime unlockedAt
    ) {}

    public record RecentCompletion(
            UUID lessonId,
            String lessonTitle,
            int xpEarned,
            int score,
            OffsetDateTime completedAt
    ) {}

    public record ReportOverview(
            UUID id,
            ReportTargetType targetType,
            UUID targetId,
            ReportReason reason,
            String details,
            ReportStatus status,
            OffsetDateTime createdAt
    ) {}
}
