package com.riksa.gamification.service;

import com.riksa.common.event.EventPublisher;
import com.riksa.common.cache.CacheNames;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.entity.XpTransaction;
import com.riksa.gamification.event.XpGainedEvent;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.repository.XpTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Hot-path service: appends XP to a user's ledger.
 * Uses pessimistic locking (PESSIMISTIC_WRITE) for the user_gamification row
 * to prevent lost updates under concurrent lesson completions.
 * After committing the XP, publishes an XpGainedEvent so that RankService
 * and AchievementService can react.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XpLedgerService {

    private final UserGamificationRepository userGamificationRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final EventPublisher eventPublisher;

    /**
     * Award XP to a user. This is the single entry point for all XP grants.
     *
     * @param userId     the user receiving XP
     * @param amount     XP to award (must be positive)
     * @param sourceType e.g. "LESSON_COMPLETION", "DAILY_CHALLENGE", "TASK_COMPLETION"
     * @param sourceId   the ID of the source entity (lesson ID, challenge ID, etc.)
     * @return the new total XP after the grant
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.GAMIFICATION_PROFILE, key = "#userId.toString()",
            condition = "@redisCacheFeatureSwitch.enabled()")
    public long awardXp(UUID userId, int amount, String sourceType, UUID sourceId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("XP amount must be positive, got: " + amount);
        }

        // Pessimistic lock — only one thread can update this user's gamification at a time
        UserGamification ug = userGamificationRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "UserGamification row not found for user " + userId + ". "
                        + "Ensure the row is created when the user registers."));

        // A completion event can be retried after an infrastructure failure.
        // Source-backed rewards must be idempotent; source-less rewards retain
        // their existing behavior because they do not identify a single action.
        if (sourceId != null && xpTransactionRepository
                .findByUserIdAndSourceTypeAndSourceId(userId, sourceType, sourceId)
                .isPresent()) {
            log.info("Skipping duplicate XP source {}:{} for user {}", sourceType, sourceId, userId);
            return ug.getTotalXp();
        }

        long newTotal = ug.getTotalXp() + amount;
        ug.setTotalXp(newTotal);
        ug.setXpBalance(ug.getXpBalance() + amount);
        userGamificationRepository.save(ug);

        // Append the transaction with running balance
        XpTransaction tx = new XpTransaction();
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setSourceType(sourceType);
        tx.setSourceId(sourceId);
        tx.setRunningBalance(newTotal);
        xpTransactionRepository.save(tx);

        log.info("Awarded {} XP to user {} (source: {}:{}) — new total: {}", amount, userId, sourceType, sourceId, newTotal);

        // Publish event for downstream consumers (rank check, achievement check)
        eventPublisher.publish(new XpGainedEvent(this, userId, amount, newTotal, sourceType, sourceId));

        return newTotal;
    }
}
