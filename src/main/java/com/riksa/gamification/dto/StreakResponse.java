package com.riksa.gamification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StreakResponse {
    private int currentStreak;
    private int highestStreak;
    private String currentMilestoneName;
    private String currentMilestoneIcon;
    private String nextMilestoneName;
    private int nextMilestoneMinStreak;
    private int daysToNext;
}
