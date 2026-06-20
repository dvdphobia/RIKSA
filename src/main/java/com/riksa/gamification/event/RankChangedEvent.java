package com.riksa.gamification.event;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

import java.util.UUID;

/**
 * Published when a user's rank/tier changes (e.g. BRONZE → SILVER).
 * AchievementService listens to this for rank-based achievements.
 * AI context listener can use this to update chat context.
 */
@Getter
public class RankChangedEvent extends DomainEvent {

    private final UUID userId;
    private final String oldRank;
    private final String newRank;
    private final String scope;

    public RankChangedEvent(Object source, UUID userId, String oldRank, String newRank, String scope) {
        super(source);
        this.userId = userId;
        this.oldRank = oldRank;
        this.newRank = newRank;
        this.scope = scope;
    }
}