package com.riksa.gamification.service;

import com.riksa.common.enums.AchievementCriteriaType;
import com.riksa.common.enums.TierType;
import com.riksa.common.event.EventPublisher;
import com.riksa.gamification.entity.Achievement;
import com.riksa.gamification.entity.UserAchievement;
import com.riksa.gamification.event.AchievementUnlockedEvent;
import com.riksa.gamification.repository.AchievementRepository;
import com.riksa.gamification.repository.UserAchievementRepository;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.social.repository.CategoryRepository;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.ReactionRepository;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.blog.repository.BlogLikeRepository;
import com.riksa.blog.enums.BlogStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Evaluates achievement criteria after events (LESSON_COMPLETED, XP_GAINED,
 * STREAK_UPDATED, RANK_CHANGED, POST_CREATED, REACTION_RECEIVED, BLOG_APPROVED, BLOG_LIKED).
 * Checks all unearned achievements of the matching criteria type and unlocks
 * any that now meet their thresholds.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final EventPublisher eventPublisher;
    private final XpLedgerService xpLedgerService;

    // ── Learning repos (for category-based criteria) ──
    private final LessonCompletionRepository lessonCompletionRepository;
    private final LessonRepository lessonRepository;
    private final CategoryRepository categoryRepository;

    // ── Social/blog repos (for social/blog criteria + progress) ──
    private final PostRepository postRepository;
    private final ReactionRepository reactionRepository;
    private final BlogPostRepository blogPostRepository;
    private final BlogLikeRepository blogLikeRepository;

    // ════════════════════════════════════════════════════
    //  XP-BASED ACHIEVEMENTS
    // ════════════════════════════════════════════════════

    @Transactional
    public void checkXpAchievements(UUID userId, long totalXp) {
        List<Achievement> xpAchievements = achievementRepository
                .findByCriteriaTypeAndActiveTrue(AchievementCriteriaType.EARN_XP);

        for (Achievement a : xpAchievements) {
            if (alreadyEarned(userId, a.getId())) continue;
            if (totalXp >= a.getCriteriaValue()) {
                unlockAchievement(userId, a);
            }
        }
    }

    // ════════════════════════════════════════════════════
    //  LESSON-COMPLETION ACHIEVEMENTS
    // ════════════════════════════════════════════════════

    @Transactional
    public void checkCourseCompletionAchievements(UUID userId, int coursesCompleted) {
        // COMPLETE_N_COURSES — threshold-based
        List<Achievement> courseAchievements = achievementRepository
                .findByCriteriaTypeAndActiveTrue(AchievementCriteriaType.COMPLETE_N_COURSES);

        for (Achievement a : courseAchievements) {
            if (alreadyEarned(userId, a.getId())) continue;
            if (coursesCompleted >= a.getCriteriaValue()) {
                unlockAchievement(userId, a);
            }
        }

        // COMPLETE_COURSE — any single lesson completion (criteriaValue ignored)
        List<Achievement> singleCourseAchievements = achievementRepository
                .findByCriteriaTypeAndActiveTrue(AchievementCriteriaType.COMPLETE_COURSE);

        for (Achievement a : singleCourseAchievements) {
            if (alreadyEarned(userId, a.getId())) continue;
            if (coursesCompleted >= 1) {
                unlockAchievement(userId, a);
            }
        }

        // COMPLETE_COURSE_IN_EVERY_CATEGORY — user completed at least 1 lesson in every category
        checkEveryCategoryAchievements(userId);

        // COMPLETE_ALL_IN_CATEGORY — user completed ALL lessons in a specific category
        checkAllInCategoryAchievements(userId);

        // COMPLETE_ALL_IN_EVERY_CATEGORY — user completed ALL lessons across ALL categories
        checkAllInEveryCategoryAchievements(userId);
    }

    @Transactional
    public void checkEveryCategoryAchievements(UUID userId) {
        List<Achievement> achievements = achievementRepository
                .findByCriteriaTypeAndActiveTrue(AchievementCriteriaType.COMPLETE_COURSE_IN_EVERY_CATEGORY);

        if (achievements.isEmpty()) return;

        long totalCategories = categoryRepository.countByOfficialTrue();
        if (totalCategories == 0) return;

        long categoriesCompleted = lessonCompletionRepository.countDistinctCategoriesCompleted(userId);

        for (Achievement a : achievements) {
            if (alreadyEarned(userId, a.getId())) continue;
            if (categoriesCompleted >= totalCategories) {
                unlockAchievement(userId, a);
            }
        }
    }

    @Transactional
    public void checkAllInCategoryAchievements(UUID userId) {
        List<Achievement> achievements = achievementRepository
                .findByCriteriaTypeAndActiveTrue(AchievementCriteriaType.COMPLETE_ALL_IN_CATEGORY);

        for (Achievement a : achievements) {
            if (alreadyEarned(userId, a.getId())) continue;
            if (a.getCategoryId() == null) continue;

            long completedInCat = lessonCompletionRepository.countCompletedInCategory(userId, a.getCategoryId());
            long totalInCat = lessonRepository.countByCategoryId(a.getCategoryId());

            if (totalInCat > 0 && completedInCat >= totalInCat) {
                unlockAchievement(userId, a);
            }
        }
    }

    @Transactional
    public void checkAllInEveryCategoryAchievements(UUID userId) {
        List<Achievement> achievements = achievementRepository
                .findByCriteriaTypeAndActiveTrue(AchievementCriteriaType.COMPLETE_ALL_IN_EVERY_CATEGORY);

        if (achievements.isEmpty()) return;

        long totalLessons = lessonRepository.count();
        if (totalLessons == 0) return;

        long totalCompleted = lessonCompletionRepository.countByUserId(userId);

        for (Achievement a : achievements) {
            if (alreadyEarned(userId, a.getId())) continue;
            if (totalCompleted >= totalLessons) {
                unlockAchievement(userId, a);
            }
        }
    }

    @Transactional
    public void checkDifficultyAchievements(UUID userId, String difficulty) {
        List<Achievement> diffAchievements = achievementRepository
                .findByCriteriaTypeAndActiveTrue(AchievementCriteriaType.COMPLETE_COURSE_WITH_DIFFICULTY);

        boolean isAdvanced = "HARD".equals(difficulty) || "DIFFICULT".equals(difficulty);

        for (Achievement a : diffAchievements) {
            if (alreadyEarned(userId, a.getId())) continue;
            // requiresAdvanced = true  → unlocks on HARD or DIFFICULT
            // requiresAdvanced = false → unlocks on EASY or MEDIUM
            if (a.isRequiresAdvanced() == isAdvanced) {
                unlockAchievement(userId, a);
            }
        }
    }

    // ════════════════════════════════════════════════════
    //  STREAK ACHIEVEMENTS
    // ════════════════════════════════════════════════════

    @Transactional
    public void checkStreakAchievements(UUID userId, long currentStreak) {
        List<Achievement> streakAchievements = achievementRepository
                .findByCriteriaTypeAndActiveTrue(AchievementCriteriaType.MAINTAIN_STREAK);

        for (Achievement a : streakAchievements) {
            if (alreadyEarned(userId, a.getId())) continue;
            if (currentStreak >= a.getCriteriaValue()) {
                unlockAchievement(userId, a);
            }
        }
    }

    // ════════════════════════════════════════════════════
    //  RANK ACHIEVEMENTS
    // ════════════════════════════════════════════════════

    @Transactional
    public void checkRankAchievements(UUID userId, String newRank) {
        List<Achievement> rankAchievements = achievementRepository
                .findByCriteriaTypeAndActiveTrue(AchievementCriteriaType.REACH_RANK);

        // Map rank name to ordinal (BRONZE=0, SILVER=1, GOLD=2, PLATINUM=3, DIAMOND=4, LEGEND=5)
        int rankOrdinal;
        try {
            rankOrdinal = TierType.valueOf(newRank).ordinal();
        } catch (IllegalArgumentException e) {
            log.warn("Unknown rank name '{}', skipping rank achievements", newRank);
            return;
        }

        for (Achievement a : rankAchievements) {
            if (alreadyEarned(userId, a.getId())) continue;
            if (a.getCriteriaValue() != null && rankOrdinal >= a.getCriteriaValue()) {
                unlockAchievement(userId, a);
            }
        }
    }

    // ════════════════════════════════════════════════════
    //  SOCIAL ACHIEVEMENTS
    // ════════════════════════════════════════════════════

    @Transactional
    public void checkPostAchievements(UUID userId, int postCount) {
        List<Achievement> postAchievements = achievementRepository
                .findByCriteriaTypeAndActiveTrue(AchievementCriteriaType.CREATE_POST);

        for (Achievement a : postAchievements) {
            if (alreadyEarned(userId, a.getId())) continue;
            if (postCount >= a.getCriteriaValue()) {
                unlockAchievement(userId, a);
            }
        }
    }

    @Transactional
    public void checkReactionAchievements(UUID userId, int totalReactions) {
        List<Achievement> reactionAchievements = achievementRepository
                .findByCriteriaTypeAndActiveTrue(AchievementCriteriaType.RECEIVE_REACTION);

        for (Achievement a : reactionAchievements) {
            if (alreadyEarned(userId, a.getId())) continue;
            if (totalReactions >= a.getCriteriaValue()) {
                unlockAchievement(userId, a);
            }
        }
    }

    // ════════════════════════════════════════════════════
    //  BLOG ACHIEVEMENTS
    // ════════════════════════════════════════════════════

    @Transactional
    public void checkBlogPostAchievements(UUID userId, int approvedBlogCount) {
        List<Achievement> blogAchievements = achievementRepository
                .findByCriteriaTypeAndActiveTrue(AchievementCriteriaType.WRITE_BLOG_POST);

        for (Achievement a : blogAchievements) {
            if (alreadyEarned(userId, a.getId())) continue;
            if (approvedBlogCount >= a.getCriteriaValue()) {
                unlockAchievement(userId, a);
            }
        }
    }

    @Transactional
    public void checkBlogLikeAchievements(UUID userId, int totalBlogLikes) {
        List<Achievement> blogLikeAchievements = achievementRepository
                .findByCriteriaTypeAndActiveTrue(AchievementCriteriaType.RECEIVE_BLOG_LIKE);

        for (Achievement a : blogLikeAchievements) {
            if (alreadyEarned(userId, a.getId())) continue;
            if (totalBlogLikes >= a.getCriteriaValue()) {
                unlockAchievement(userId, a);
            }
        }
    }

    // ════════════════════════════════════════════════════
    //  CORE UNLOCK LOGIC
    // ════════════════════════════════════════════════════

    private boolean alreadyEarned(UUID userId, UUID achievementId) {
        return userAchievementRepository.existsByUserIdAndAchievementId(userId, achievementId);
    }

    private void unlockAchievement(UUID userId, Achievement achievement) {
        UserAchievement ua = new UserAchievement();
        ua.setUserId(userId);
        ua.setAchievementId(achievement.getId());
        userAchievementRepository.save(ua);

        log.info("Achievement unlocked: '{}' for user {} (XP reward: {})",
                achievement.getName(), userId, achievement.getXpReward());

        // Award bonus XP if the achievement has an XP reward
        if (achievement.getXpReward() > 0) {
            try {
                xpLedgerService.awardXp(userId, achievement.getXpReward(),
                        "ACHIEVEMENT_UNLOCK", achievement.getId());
            } catch (Exception e) {
                log.error("Failed to award XP reward for achievement '{}' to user {}: {}",
                        achievement.getName(), userId, e.getMessage(), e);
            }
        }

        eventPublisher.publish(new AchievementUnlockedEvent(this, userId,
                achievement.getId(), achievement.getName()));
    }
}
