package com.riksa.leaderboard.listener;

import com.riksa.auth.event.UserProfileUpdatedEvent;
import com.riksa.gamification.event.StreakUpdatedEvent;
import com.riksa.gamification.event.XpGainedEvent;
import com.riksa.leaderboard.service.RedisLeaderboardProjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLeaderboardListener {

    private final RedisLeaderboardProjector projector;

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onXpGained(XpGainedEvent event) {
        update(event.getUserId());
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStreakUpdated(StreakUpdatedEvent event) {
        update(event.getUserId());
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfileUpdated(UserProfileUpdatedEvent event) {
        update(event.getUserId());
    }

    private void update(java.util.UUID userId) {
        try {
            projector.update(userId);
        } catch (RuntimeException e) {
            log.error("Failed to update Redis leaderboard for user {}", userId, e);
        }
    }
}
