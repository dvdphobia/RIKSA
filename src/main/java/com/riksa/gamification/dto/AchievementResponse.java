package com.riksa.gamification.dto;

import com.riksa.common.enums.AchievementRarity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AchievementResponse {
    private UUID id;
    private String name;
    private String description;
    private String iconUrl;
    private boolean earned;
    private OffsetDateTime unlockedAt;
    private int progress;
    private int progressMax;
    private double progressPercent;
    private AchievementRarity rarity;
    private int xpReward;
}