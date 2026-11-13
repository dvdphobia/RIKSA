package com.riksa.learning.event;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Published when a user completes a lesson.
 * Gamification listeners (XpLedger, Streak, Achievement) consume this
 * to award XP, update streaks, and check achievements.
 */
@Getter
public class LessonCompletedEvent extends DomainEvent {

    private final UUID userId;
    private final UUID lessonId;
    private final int xpEarned;
    private final OffsetDateTime completedAt;

    public LessonCompletedEvent(Object source, UUID userId, UUID lessonId, int xpEarned) {
        this(source, userId, lessonId, xpEarned, OffsetDateTime.now());
    }

    public LessonCompletedEvent(Object source, UUID userId, UUID lessonId, int xpEarned,
                                OffsetDateTime completedAt) {
        super(source);
        this.userId = userId;
        this.lessonId = lessonId;
        this.xpEarned = xpEarned;
        this.completedAt = completedAt;
    }
}
