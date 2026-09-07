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
    private long currentStreak;
    private long highestStreak;
    private String currentTier;
    private String currentTierIcon;
    /** XP at which the current tier begins, so clients can measure progress through the band. */
    private long currentTierMinXp;
    private String nextTier;
    /** XP at which the next tier begins. Zero when the user is already at the highest tier. */
    private long nextTierMinXp;
    private long xpToNext;
}