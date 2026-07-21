package com.riksa.gamification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StreakResponse {
    private long currentStreak;
    private long highestStreak;
    private String currentMilestoneName;
    private String currentMilestoneIcon;
    private String nextMilestoneName;
    private long nextMilestoneMinStreak;
    private int daysToNext;
}
