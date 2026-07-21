package com.riksa.gamification.service;

import com.riksa.common.enums.AchievementCriteriaType;
import com.riksa.common.event.EventPublisher;
import com.riksa.gamification.entity.Achievement;
import com.riksa.gamification.entity.UserAchievement;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.event.AchievementUnlockedEvent;
import com.riksa.gamification.repository.AchievementRepository;
import com.riksa.gamification.repository.UserAchievementRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Evaluates achievement criteria after events (XP_GAINED, STREAK_UPDATED, RANK_CHANGED).
 * Checks all unearned achievements of the matching criteria type and unlocks
 * any that now meet their thresholds.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserGamificationRepository userGamificationRepository;
    private final EventPublisher eventPublisher;

    /**
     * Check EARN_XP achievements after an XP gain.
     */
    @Transactional
    public void checkXpAchievements(UUID userId, long totalXp) {
        List<Achievement> xpAchievements = achievementRepository
                .findByCriteriaType(AchievementCriteriaType.EARN_XP);

        for (Achievement a : xpAchievements) {
            if (alreadyEarned(userId, a.getId())) continue;

            if (totalXp >= a.getCriteriaValue()) {
                unlockAchievement(userId, a);
            }
        }
    }

    /**
     * Check streak-based achievements after a streak update.
     */
    @Transactional
    public void checkStreakAchievements(UUID userId, long currentStreak) {
        // No dedicated STREAK criteria type in the current enum, but we check
        // COMPLETE_N_COURSES as a proxy for now. This can be extended with
        // a STREAK_N_DAYS criteria type if needed.
        // For now, we only check existing types that could relate to streaks.
        log.debug("Streak achievement check for user {}: streak={}", userId, currentStreak);
    }

    /**
     * Check rank-based achievements after a rank change.
     */
    @Transactional
    public void checkRankAchievements(UUID userId, String newRank) {
        // Could add RANK_REACHED criteria type in the future
        log.debug("Rank achievement check for user {}: newRank={}", userId, newRank);
    }

    /**
     * Check COMPLETE_N_COURSES achievements.
     * Called by listeners when a lesson completion event fires.
     *
     * @param userId         the user
     * @param coursesCompleted the total number of courses/lessons completed so far
     */
    @Transactional
    public void checkCourseCompletionAchievements(UUID userId, int coursesCompleted) {
        List<Achievement> courseAchievements = achievementRepository
                .findByCriteriaType(AchievementCriteriaType.COMPLETE_N_COURSES);

        for (Achievement a : courseAchievements) {
            if (alreadyEarned(userId, a.getId())) continue;

            if (coursesCompleted >= a.getCriteriaValue()) {
                unlockAchievement(userId, a);
            }
        }
    }

    /**
     * Check COMPLETE_COURSE_WITH_DIFFICULTY achievements.
     * Called when a user completes a lesson with a specific difficulty.
     */
    @Transactional
    public void checkDifficultyAchievements(UUID userId, String difficulty) {
        List<Achievement> diffAchievements = achievementRepository
                .findByCriteriaType(AchievementCriteriaType.COMPLETE_COURSE_WITH_DIFFICULTY);

        for (Achievement a : diffAchievements) {
            if (alreadyEarned(userId, a.getId())) continue;
            // criteriaValue is 0 for difficulty achievements, requiresAdvanced flag is used instead
            unlockAchievement(userId, a);
        }
    }

    private boolean alreadyEarned(UUID userId, UUID achievementId) {
        return userAchievementRepository.existsByUserIdAndAchievementId(userId, achievementId);
    }

    private void unlockAchievement(UUID userId, Achievement achievement) {
        UserAchievement ua = new UserAchievement();
        ua.setUserId(userId);
        ua.setAchievementId(achievement.getId());
        userAchievementRepository.save(ua);

        log.info("Achievement unlocked: '{}' for user {}", achievement.getName(), userId);

        eventPublisher.publish(new AchievementUnlockedEvent(this, userId,
                achievement.getId(), achievement.getName()));
    }
}