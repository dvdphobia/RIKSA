package com.riksa.gamification.event;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

import java.util.UUID;

/**
 * Published when a user's streak is updated (incremented or reset).
 * AchievementService listens to this to check streak-based achievements.
 */
@Getter
public class StreakUpdatedEvent extends DomainEvent {

    private final UUID userId;
    private final long currentStreak;
    private final long highestStreak;

    public StreakUpdatedEvent(Object source, UUID userId, long currentStreak, long highestStreak) {
        super(source);
        this.userId = userId;
        this.currentStreak = currentStreak;
        this.highestStreak = highestStreak;
    }
}