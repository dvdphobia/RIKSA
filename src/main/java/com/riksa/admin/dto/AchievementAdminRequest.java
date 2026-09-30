package com.riksa.admin.dto;

import com.riksa.common.enums.AchievementCriteriaType;
import com.riksa.common.enums.AchievementRarity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AchievementAdminRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank String description,
        @NotNull AchievementCriteriaType criteriaType,
        @Min(0) Long criteriaValue,
        @NotNull Boolean requiresAdvanced,
        @NotBlank String iconUrl,
        UUID categoryId,
        @NotNull AchievementRarity rarity,
        @NotNull @Min(0) Integer xpReward,
        @NotNull Boolean active) {
}
