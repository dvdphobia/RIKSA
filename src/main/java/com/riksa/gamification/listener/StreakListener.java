package com.riksa.gamification.listener;

import com.riksa.gamification.service.StreakService;
import com.riksa.learning.event.LessonCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLessonCompleted(LessonCompletedEvent event) {
        log.info("Updating streak for user {} after lesson completion", event.getUserId());

        streakService.updateStreak(event.getUserId());
    }
}
