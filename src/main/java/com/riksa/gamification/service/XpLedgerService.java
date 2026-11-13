package com.riksa.gamification.service;

import com.riksa.admin.service.SettingsService;
import com.riksa.common.event.EventPublisher;
import com.riksa.common.cache.CacheNames;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.entity.XpBoost;
import com.riksa.gamification.entity.XpTransaction;
import com.riksa.gamification.event.XpGainedEvent;
import com.riksa.gamification.repository.XpBoostRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.repository.XpTransactionRepository;
import com.riksa.quest.service.QuestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;
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
    private final XpBoostRepository xpBoostRepository;
    private final SettingsService settingsService;
    private final EventPublisher eventPublisher;

    private static final java.time.ZoneId CAMBODIA_ZONE = QuestService.DAY_ZONE;

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
        return awardXpAndGetResult(userId, amount, sourceType, sourceId).totalXp();
    }

    /**
     * Award XP and report the amount actually recorded after boosts and caps.
     * Source-backed retries return the amount from the original ledger entry.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.GAMIFICATION_PROFILE, key = "#userId.toString()",
            condition = "@redisCacheFeatureSwitch.enabled()")
    public XpAwardResult awardXpAndGetResult(UUID userId, int amount, String sourceType, UUID sourceId) {
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
        var existingTransaction = sourceId == null ? java.util.Optional.<XpTransaction>empty()
                : xpTransactionRepository.findByUserIdAndSourceTypeAndSourceId(userId, sourceType, sourceId);
        if (existingTransaction.isPresent()) {
            log.info("Skipping duplicate XP source {}:{} for user {}", sourceType, sourceId, userId);
            return new XpAwardResult(Math.toIntExact(existingTransaction.get().getAmount()), ug.getTotalXp());
        }

        int awardedAmount = applyActiveBoost(amount, sourceType);
        if ("LESSON_COMPLETION".equals(sourceType)) {
            awardedAmount = applyDailyLessonCap(userId, awardedAmount);
        }

        long newTotal = ug.getTotalXp() + awardedAmount;
        if (awardedAmount > 0) {
            ug.setTotalXp(newTotal);
            ug.setXpBalance(ug.getXpBalance() + awardedAmount);
            userGamificationRepository.save(ug);
        }

        // Append the transaction with running balance
        XpTransaction tx = new XpTransaction();
        tx.setUserId(userId);
        tx.setAmount(awardedAmount);
        tx.setSourceType(sourceType);
        tx.setSourceId(sourceId);
        tx.setRunningBalance(newTotal);
        xpTransactionRepository.save(tx);

        log.info("Recorded {} XP for user {} (source: {}:{}) — new total: {}", awardedAmount, userId, sourceType, sourceId, newTotal);

        // Publish event for downstream consumers (rank check, achievement check)
        if (awardedAmount > 0) {
            eventPublisher.publish(new XpGainedEvent(this, userId, awardedAmount, newTotal, sourceType, sourceId));
        }

        return new XpAwardResult(awardedAmount, newTotal);
    }

    private int applyActiveBoost(int baseAmount, String sourceType) {
        if (!isBoostEligible(baseAmount, sourceType)) return baseAmount;
        OffsetDateTime now = OffsetDateTime.now();
        return xpBoostRepository.findFirstByStartsAtLessThanEqualAndEndsAtGreaterThanOrderByStartsAtDesc(now, now)
                .map(XpBoost::getMultiplier)
                .map(multiplier -> multiplier.multiply(BigDecimal.valueOf(baseAmount))
                        .setScale(0, RoundingMode.HALF_UP).intValueExact())
                .orElse(baseAmount);
    }

    private int applyDailyLessonCap(UUID userId, int boostedAmount) {
        int cap = settingsService.getXpRules().dailyLessonXpCap();
        if (cap <= 0) return boostedAmount;

        LocalDate today = LocalDate.now(CAMBODIA_ZONE);
        OffsetDateTime dayStart = today.atStartOfDay(CAMBODIA_ZONE).toOffsetDateTime();
        OffsetDateTime nextDayStart = today.plusDays(1).atStartOfDay(CAMBODIA_ZONE).toOffsetDateTime();
        long alreadyEarned = xpTransactionRepository.sumAmountByUserAndSourceTypeInWindow(
                userId, "LESSON_COMPLETION", dayStart, nextDayStart);
        long remaining = Math.max(0L, (long) cap - alreadyEarned);
        return (int) Math.min(boostedAmount, remaining);
    }

    private boolean isBoostEligible(int amount, String sourceType) {
        if (amount <= 0 || sourceType == null) return false;
        String normalized = sourceType.toUpperCase(Locale.ROOT);
        return !normalized.startsWith("ADMIN_ADJUST")
                && !normalized.contains("MARKET")
                && !normalized.contains("SPEND");
    }

    public record XpAwardResult(int amountAwarded, long totalXp) {}
}
