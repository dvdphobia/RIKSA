package com.riksa.leaderboard.event;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Published after the leaderboard snapshot job completes a refresh cycle.
 * AI context listener can consume this to invalidate cached context.
 */
@Getter
public class LeaderboardRefreshedEvent extends DomainEvent {

    private final LocalDate snapshotDate;
    private final int snapshotsCreated;

    public LeaderboardRefreshedEvent(Object source, LocalDate snapshotDate, int snapshotsCreated) {
        super(source);
        this.snapshotDate = snapshotDate;
        this.snapshotsCreated = snapshotsCreated;
    }
}