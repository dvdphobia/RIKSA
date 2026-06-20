package com.riksa.gamification.service;

import com.riksa.common.event.EventPublisher;
import com.riksa.common.enums.ScopeType;
import com.riksa.common.enums.TierType;
import com.riksa.gamification.entity.RankHistory;
import com.riksa.gamification.entity.RankTier;
import com.riksa.gamification.event.RankChangedEvent;
import com.riksa.gamification.repository.RankHistoryRepository;
import com.riksa.gamification.repository.RankTierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Checks whether a user's XP total qualifies them for a new rank tier.
 * If a tier change is detected, inserts a RankHistory row and publishes
 * a RankChangedEvent so that AchievementService can react.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankService {

    private final RankTierRepository rankTierRepository;
    private final RankHistoryRepository rankHistoryRepository;
    private final EventPublisher eventPublisher;

    /**
     * Check and update the user's rank after an XP gain.
     * Called by RankListener in response to an XpGainedEvent.
     *
     * @param userId    the user whose rank to check
     * @param newTotalXp the user's new total XP
     */
    @Transactional
    public void checkAndPromoteRank(UUID userId, long newTotalXp) {
        // Find the tier the user now qualifies for (highest tier where minXp <= totalXp)
        RankTier newTier = rankTierRepository
                .findFirstByMinXpLessThanEqualOrderByMinXpDesc(newTotalXp)
                .orElse(null);

        if (newTier == null) {
            log.warn("No rank tier found for user {} with XP {}", userId, newTotalXp);
            return;
        }

        // Determine the user's previous rank by looking at their latest rank history entry
        TierType previousRank = inferPreviousRank(userId);

        if (newTier.getTierName() != previousRank) {
            // Rank change detected!
            RankHistory history = new RankHistory();
            history.setUserId(userId);
            history.setScope(ScopeType.COUNTRY);  // Currently only tracking COUNTRY scope
            history.setOldRank(previousRank);
            history.setNewRank(newTier.getTierName());
            rankHistoryRepository.save(history);

            log.info("User {} ranked up: {} → {} (XP: {})", userId, previousRank, newTier.getTierName(), newTotalXp);

            eventPublisher.publish(new RankChangedEvent(this, userId,
                    previousRank.name(), newTier.getTierName().name(), ScopeType.COUNTRY.name()));
        }
    }

    private TierType inferPreviousRank(UUID userId) {
        return rankHistoryRepository.findByUserIdOrderByChangedAtDesc(userId)
                .stream()
                .findFirst()
                .map(RankHistory::getNewRank)
                .orElse(TierType.BRONZE);  // Default: everyone starts at BRONZE
    }
}