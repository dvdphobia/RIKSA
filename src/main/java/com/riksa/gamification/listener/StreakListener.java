package com.riksa.gamification.listener;

import com.riksa.gamification.service.StreakService;
import com.riksa.learning.event.LessonCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for LESSON_COMPLETED events and updates the user's streak.
 * Runs before the completion transaction commits so the streak cannot be lost
 * after the lesson completion has become visible.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreakListener {

    private final StreakService streakService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleLessonCompleted(LessonCompletedEvent event) {
        log.info("Updating streak for user {} after lesson completion", event.getUserId());

        streakService.updateStreak(event.getUserId(), event.getCompletedAt());
    }
}
