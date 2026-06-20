package com.riksa.gamification.listener;

import com.riksa.gamification.event.RankChangedEvent;
import com.riksa.gamification.event.StreakUpdatedEvent;
import com.riksa.gamification.event.XpGainedEvent;
import com.riksa.gamification.service.AchievementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for XP_GAINED, STREAK_UPDATED, and RANK_CHANGED events
 * and checks whether any achievements should be unlocked.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AchievementListener {

    private final AchievementService achievementService;

    @EventListener
    public void handleXpGained(XpGainedEvent event) {
        log.info("Checking XP achievements for user {}", event.getUserId());
        try {
            achievementService.checkXpAchievements(event.getUserId(), event.getNewTotalXp());
        } catch (Exception e) {
            log.error("Failed to check XP achievements for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }

    @EventListener
    public void handleStreakUpdated(StreakUpdatedEvent event) {
        log.info("Checking streak achievements for user {}", event.getUserId());
        try {
            achievementService.checkStreakAchievements(event.getUserId(), event.getCurrentStreak());
        } catch (Exception e) {
            log.error("Failed to check streak achievements for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }

    @EventListener
    public void handleRankChanged(RankChangedEvent event) {
        log.info("Checking rank achievements for user {}", event.getUserId());
        try {
            achievementService.checkRankAchievements(event.getUserId(), event.getNewRank());
        } catch (Exception e) {
            log.error("Failed to check rank achievements for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }
}