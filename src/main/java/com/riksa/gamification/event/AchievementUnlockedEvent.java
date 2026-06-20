package com.riksa.gamification.event;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

import java.util.UUID;

/**
 * Published when a user unlocks a new achievement.
 * AI context listener can use this to update chat context.
 */
@Getter
public class AchievementUnlockedEvent extends DomainEvent {

    private final UUID userId;
    private final UUID achievementId;
    private final String achievementName;

    public AchievementUnlockedEvent(Object source, UUID userId, UUID achievementId, String achievementName) {
        super(source);
        this.userId = userId;
        this.achievementId = achievementId;
        this.achievementName = achievementName;
    }
}