package com.riksa.gamification.service;

import com.riksa.common.event.EventPublisher;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.event.StreakUpdatedEvent;
import com.riksa.gamification.repository.UserGamificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Tracks daily streaks. A streak increments when a user completes a lesson
 * on a new calendar day (Asia/Phnom_Penh). If they miss a day, the streak resets to 0.
 * The streak day window closes at 23:59:59 Cambodia time.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreakService {

    private static final ZoneId CAMBODIA_TZ = ZoneId.of("Asia/Phnom_Penh");

    private final UserGamificationRepository userGamificationRepository;
    private final EventPublisher eventPublisher;

    /**
     * Update the user's streak after a lesson completion.
     * Called inside the same transaction as XP awarding (by StreakListener).
     *
     * Streak logic:
     * - If this is the user's first completion ever → streak = 1
     * - If the last completion was today (Cambodia time) → no change (same day)
     * - If the last completion was yesterday (Cambodia time) → streak += 1
     * - If the last completion was >1 day ago → streak resets to 1
     *
     * @return the updated streak count
     */
    @Transactional
    public long updateStreak(UUID userId) {
        UserGamification ug = userGamificationRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "UserGamification row not found for user " + userId));

        OffsetDateTime now = OffsetDateTime.now(CAMBODIA_TZ);
        OffsetDateTime lastCompleted = ug.getLastCompletedAt();

        long newStreak;

        if (lastCompleted == null) {
            // First completion ever
            newStreak = 1;
        } else {
            OffsetDateTime lastInCambodia = lastCompleted.atZoneSameInstant(CAMBODIA_TZ).toOffsetDateTime();
            int daysBetween = now.toLocalDate().compareTo(lastInCambodia.toLocalDate());

            if (daysBetween == 0) {
                // Same day — no change
                log.debug("User {} completed on same day, streak unchanged: {}", userId, ug.getCurrentStreak());
                return ug.getCurrentStreak();
            } else if (daysBetween == 1) {
                // Next day — streak increments
                newStreak = ug.getCurrentStreak() + 1;
            } else {
                // Missed one or more days — streak resets
                newStreak = 1;
            }
        }

        ug.setCurrentStreak(newStreak);
        if (newStreak > ug.getHighestStreak()) {
            ug.setHighestStreak(newStreak);
        }
        ug.setLastCompletedAt(now);
        userGamificationRepository.save(ug);

        log.info("User {} streak updated: {} → {}", userId, ug.getCurrentStreak(), newStreak);

        eventPublisher.publish(new StreakUpdatedEvent(this, userId, newStreak, ug.getHighestStreak()));

        return newStreak;
    }

    /**
     * Batch-reset stale streaks (called by a scheduled job).
     * Finds all users whose lastCompletedAt is older than yesterday's start
     * and resets their currentStreak to 0.
     */
    @Transactional
    public int resetStaleStreaks() {
        OffsetDateTime yesterdayStart = OffsetDateTime.now(CAMBODIA_TZ)
                .minusDays(1)
                .toLocalDate()
                .atStartOfDay(CAMBODIA_TZ)
                .toOffsetDateTime();

        var staleUsers = userGamificationRepository.findByLastCompletedAtBefore(yesterdayStart);
        int count = 0;
        for (UserGamification ug : staleUsers) {
            if (ug.getCurrentStreak() > 0) {
                ug.setCurrentStreak(0);
                userGamificationRepository.save(ug);
                count++;
            }
        }
        log.info("Reset {} stale streaks", count);
        return count;
    }
}