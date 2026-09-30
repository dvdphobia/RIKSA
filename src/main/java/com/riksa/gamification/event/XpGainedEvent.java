package com.riksa.gamification.event;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

import java.util.UUID;

/**
 * Published when a user earns XP (e.g. lesson completion, daily challenge).
 * RankService listens to this to check for tier changes.
 * AchievementService listens to this to check EARN_XP criteria.
 */
@Getter
public class XpGainedEvent extends DomainEvent {

    private final UUID userId;
    private final int amount;
    private final long newTotalXp;
    private final String sourceType;
    private final UUID sourceId;

    public XpGainedEvent(Object source, UUID userId, int amount, long newTotalXp,
                         String sourceType, UUID sourceId) {
        super(source);
        this.userId = userId;
        this.amount = amount;
        this.newTotalXp = newTotalXp;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
    }
}