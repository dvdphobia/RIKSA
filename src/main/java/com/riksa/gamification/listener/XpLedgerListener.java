package com.riksa.gamification.listener;

import com.riksa.gamification.service.XpLedgerService;
import com.riksa.learning.event.LessonCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

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

    @EventListener
    public void handleLessonCompleted(LessonCompletedEvent event) {
        log.info("Processing LESSON_COMPLETED for user {}, lesson {}, XP: {}",
                event.getUserId(), event.getLessonId(), event.getXpEarned());

        try {
            xpLedgerService.awardXp(
                    event.getUserId(),
                    event.getXpEarned(),
                    "LESSON_COMPLETION",
                    event.getLessonId()
            );
        } catch (Exception e) {
            // Log but don't rethrow — we don't want to roll back the lesson completion
            // if the XP grant fails. The streak/rank/achievement pipeline can retry later.
            log.error("Failed to award XP for user {} lesson {}: {}",
                    event.getUserId(), event.getLessonId(), e.getMessage(), e);
        }
    }
}