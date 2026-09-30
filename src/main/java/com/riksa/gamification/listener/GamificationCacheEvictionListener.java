package com.riksa.gamification.listener;

import com.riksa.common.cache.CacheNames;
import com.riksa.gamification.event.AchievementUnlockedEvent;
import com.riksa.gamification.event.StreakUpdatedEvent;
import com.riksa.gamification.event.XpGainedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GamificationCacheEvictionListener {

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onXpGained(XpGainedEvent event) {
        evict(CacheNames.GAMIFICATION_PROFILE, event.getUserId());
        evict(CacheNames.GAMIFICATION_ACHIEVEMENTS, event.getUserId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStreakUpdated(StreakUpdatedEvent event) {
        evict(CacheNames.GAMIFICATION_PROFILE, event.getUserId());
        evict(CacheNames.GAMIFICATION_STREAK, event.getUserId());
        evict(CacheNames.GAMIFICATION_ACHIEVEMENTS, event.getUserId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAchievementUnlocked(AchievementUnlockedEvent event) {
        evict(CacheNames.GAMIFICATION_ACHIEVEMENTS, event.getUserId());
    }

    private void evict(String cacheName, UUID userId) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.evict(userId.toString());
    }
}
