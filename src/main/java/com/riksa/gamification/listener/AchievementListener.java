package com.riksa.gamification.listener;

import com.riksa.gamification.event.RankChangedEvent;
import com.riksa.gamification.event.StreakUpdatedEvent;
import com.riksa.gamification.event.XpGainedEvent;
import com.riksa.gamification.service.AchievementService;
import com.riksa.learning.event.LessonCompletedEvent;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for LESSON_COMPLETED, XP_GAINED, STREAK_UPDATED, and RANK_CHANGED events
 * and checks whether any achievements should be unlocked.
 *
 * Social/blog achievements are handled separately by {@link SocialAchievementListener}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AchievementListener {

    private final AchievementService achievementService;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final LessonRepository lessonRepository;

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLessonCompleted(LessonCompletedEvent event) {
        log.info("Checking lesson-completion achievements for user {}", event.getUserId());
        try {
            // Count total lessons completed by this user
            long coursesCompleted = lessonCompletionRepository.countByUserId(event.getUserId());
            achievementService.checkCourseCompletionAchievements(event.getUserId(), (int) coursesCompleted);

            // Fetch the lesson to get its difficulty, then check difficulty achievements
            lessonRepository.findById(event.getLessonId()).ifPresent(lesson ->
                    achievementService.checkDifficultyAchievements(
                            event.getUserId(), lesson.getDifficulty().name()));
        } catch (Exception e) {
            log.error("Failed to check lesson-completion achievements for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }

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
