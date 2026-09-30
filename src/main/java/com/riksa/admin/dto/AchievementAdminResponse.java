package com.riksa.admin.dto;

import com.riksa.common.enums.AchievementCriteriaType;
import com.riksa.common.enums.AchievementRarity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AchievementAdminResponse(
        UUID id,
        String name,
        String description,
        AchievementCriteriaType criteriaType,
        Long criteriaValue,
        boolean requiresAdvanced,
        String iconUrl,
        UUID categoryId,
        AchievementRarity rarity,
        int xpReward,
        boolean active,
        long earnedCount,
        OffsetDateTime createdAt) {
}
