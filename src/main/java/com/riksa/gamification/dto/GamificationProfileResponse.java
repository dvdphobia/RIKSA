package com.riksa.gamification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class GamificationProfileResponse {
    private UUID userId;
    private long totalXp;
    private int currentStreak;
    private int highestStreak;
    private String currentTier;
    private String currentTierIcon;
    private String nextTier;
    private long xpToNext;
}