package com.riksa.gamification.listener;

import com.riksa.gamification.service.XpLedgerService;
import com.riksa.learning.event.LessonCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for LESSON_COMPLETED events and awards XP.
 * This is the entry point of the gamification hot path:
 * Lesson completed → XP awarded → XpGainedEvent published.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XpLedgerListener {

    private final XpLedgerService xpLedgerService;

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLessonCompleted(LessonCompletedEvent event) {
        log.info("Processing LESSON_COMPLETED for user {}, lesson {}, XP: {}",
                event.getUserId(), event.getLessonId(), event.getXpEarned());

        xpLedgerService.awardXp(
                event.getUserId(),
                event.getXpEarned(),
                "LESSON_COMPLETION",
                event.getLessonId()
        );
    }
}
