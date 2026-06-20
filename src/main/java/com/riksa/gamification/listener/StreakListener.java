package com.riksa.gamification.listener;

import com.riksa.gamification.service.StreakService;
import com.riksa.learning.event.LessonCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for LESSON_COMPLETED events and updates the user's streak.
 * Runs independently from XP awarding — a streak update failure
 * should not block the XP grant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreakListener {

    private final StreakService streakService;

    @EventListener
    public void handleLessonCompleted(LessonCompletedEvent event) {
        log.info("Updating streak for user {} after lesson completion", event.getUserId());

        try {
            streakService.updateStreak(event.getUserId());
        } catch (Exception e) {
            log.error("Failed to update streak for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }
}