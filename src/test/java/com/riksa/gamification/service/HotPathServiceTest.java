package com.riksa.gamification.service;

import com.riksa.common.event.EventPublisher;
import com.riksa.common.enums.AchievementCriteriaType;
import com.riksa.common.enums.ScopeType;
import com.riksa.common.enums.TierType;
import com.riksa.gamification.entity.*;
import com.riksa.gamification.event.XpGainedEvent;
import com.riksa.gamification.event.StreakUpdatedEvent;
import com.riksa.gamification.event.RankChangedEvent;
import com.riksa.gamification.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class XpLedgerServiceTest {

    @Mock private UserGamificationRepository userGamificationRepository;
    @Mock private XpTransactionRepository xpTransactionRepository;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks private XpLedgerService xpLedgerService;

    @Test
    void awardXp_incrementsTotalAndPublishesEvent() {
        UUID userId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        UserGamification ug = new UserGamification();
        ug.setUserId(userId);
        ug.setTotalXp(100);

        when(userGamificationRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(ug));

        long newTotal = xpLedgerService.awardXp(userId, 50, "LESSON_COMPLETION", lessonId);

        assertThat(newTotal).isEqualTo(150);
        assertThat(ug.getTotalXp()).isEqualTo(150);
        verify(userGamificationRepository).save(ug);
        verify(xpTransactionRepository).save(any(XpTransaction.class));

        ArgumentCaptor<XpGainedEvent> eventCaptor = ArgumentCaptor.forClass(XpGainedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        XpGainedEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getAmount()).isEqualTo(50);
        assertThat(event.getNewTotalXp()).isEqualTo(150);
        assertThat(event.getSourceType()).isEqualTo("LESSON_COMPLETION");
    }

    @Test
    void awardXp_zeroAmount_throwsException() {
        assertThatThrownBy(() -> xpLedgerService.awardXp(UUID.randomUUID(), 0, "LESSON", UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void awardXp_negativeAmount_throwsException() {
        assertThatThrownBy(() -> xpLedgerService.awardXp(UUID.randomUUID(), -10, "LESSON", UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void awardXp_userNotFound_throwsIllegalState() {
        UUID userId = UUID.randomUUID();
        when(userGamificationRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> xpLedgerService.awardXp(userId, 50, "LESSON", UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UserGamification row not found");
    }

    @Test
    void awardXp_setsRunningBalanceOnTransaction() {
        UUID userId = UUID.randomUUID();
        UserGamification ug = new UserGamification();
        ug.setUserId(userId);
        ug.setTotalXp(200);

        when(userGamificationRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(ug));

        xpLedgerService.awardXp(userId, 75, "DAILY_CHALLENGE", UUID.randomUUID());

        ArgumentCaptor<XpTransaction> txCaptor = ArgumentCaptor.forClass(XpTransaction.class);
        verify(xpTransactionRepository).save(txCaptor.capture());
        XpTransaction tx = txCaptor.getValue();
        assertThat(tx.getRunningBalance()).isEqualTo(275);
        assertThat(tx.getAmount()).isEqualTo(75);
        assertThat(tx.getSourceType()).isEqualTo("DAILY_CHALLENGE");
    }
}

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock private UserGamificationRepository userGamificationRepository;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks private StreakService streakService;

    @Test
    void updateStreak_firstCompletion_setsStreakTo1() {
        UUID userId = UUID.randomUUID();
        UserGamification ug = new UserGamification();
        ug.setUserId(userId);
        ug.setCurrentStreak(0);
        ug.setHighestStreak(0);
        ug.setLastCompletedAt(null);

        when(userGamificationRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(ug));

        int result = streakService.updateStreak(userId);

        assertThat(result).isEqualTo(1);
        assertThat(ug.getCurrentStreak()).isEqualTo(1);
        assertThat(ug.getHighestStreak()).isEqualTo(1);
        assertThat(ug.getLastCompletedAt()).isNotNull();
        verify(eventPublisher).publish(any(StreakUpdatedEvent.class));
    }

    @Test
    void updateStreak_nextDay_increments() {
        UUID userId = UUID.randomUUID();
        UserGamification ug = new UserGamification();
        ug.setUserId(userId);
        ug.setCurrentStreak(5);
        ug.setHighestStreak(10);
        // Last completed yesterday in Cambodia time
        ug.setLastCompletedAt(OffsetDateTime.now(ZoneId.of("Asia/Phnom_Penh")).minusDays(1));

        when(userGamificationRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(ug));

        int result = streakService.updateStreak(userId);

        assertThat(result).isEqualTo(6);
        assertThat(ug.getCurrentStreak()).isEqualTo(6);
        assertThat(ug.getHighestStreak()).isEqualTo(10); // Not updated since 6 < 10
    }

    @Test
    void updateStreak_sameDay_noChange() {
        UUID userId = UUID.randomUUID();
        UserGamification ug = new UserGamification();
        ug.setUserId(userId);
        ug.setCurrentStreak(5);
        ug.setHighestStreak(10);
        ug.setLastCompletedAt(OffsetDateTime.now(ZoneId.of("Asia/Phnom_Penh")));

        when(userGamificationRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(ug));

        int result = streakService.updateStreak(userId);

        assertThat(result).isEqualTo(5); // No change
        verify(userGamificationRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void updateStreak_missedDay_resetsTo1() {
        UUID userId = UUID.randomUUID();
        UserGamification ug = new UserGamification();
        ug.setUserId(userId);
        ug.setCurrentStreak(15);
        ug.setHighestStreak(30);
        // Last completed 3 days ago
        ug.setLastCompletedAt(OffsetDateTime.now(ZoneId.of("Asia/Phnom_Penh")).minusDays(3));

        when(userGamificationRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(ug));

        int result = streakService.updateStreak(userId);

        assertThat(result).isEqualTo(1);
        assertThat(ug.getCurrentStreak()).isEqualTo(1);
        assertThat(ug.getHighestStreak()).isEqualTo(30); // Preserved
    }
}

@ExtendWith(MockitoExtension.class)
class RankServiceTest {

    @Mock private RankTierRepository rankTierRepository;
    @Mock private RankHistoryRepository rankHistoryRepository;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks private RankService rankService;

    @Test
    void checkAndPromoteRank_tierUp_publishesRankChanged() {
        UUID userId = UUID.randomUUID();

        RankTier silver = new RankTier();
        silver.setTierName(TierType.SILVER);
        silver.setMinXp(100);

        // No previous history — starts at BRONZE
        when(rankTierRepository.findFirstByMinXpLessThanEqualOrderByMinXpDesc(150L))
                .thenReturn(Optional.of(silver));
        when(rankHistoryRepository.findByUserIdOrderByChangedAtDesc(userId)).thenReturn(List.of());

        rankService.checkAndPromoteRank(userId, 150);

        verify(rankHistoryRepository).save(any(RankHistory.class));
        ArgumentCaptor<RankChangedEvent> eventCaptor = ArgumentCaptor.forClass(RankChangedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getOldRank()).isEqualTo("BRONZE");
        assertThat(eventCaptor.getValue().getNewRank()).isEqualTo("SILVER");
    }

    @Test
    void checkAndPromoteRank_sameTier_noChange() {
        UUID userId = UUID.randomUUID();

        RankTier bronze = new RankTier();
        bronze.setTierName(TierType.BRONZE);
        bronze.setMinXp(0);

        when(rankTierRepository.findFirstByMinXpLessThanEqualOrderByMinXpDesc(50L))
                .thenReturn(Optional.of(bronze));
        // Previous history says they're already BRONZE
        RankHistory prevHistory = new RankHistory();
        prevHistory.setNewRank(TierType.BRONZE);
        when(rankHistoryRepository.findByUserIdOrderByChangedAtDesc(userId)).thenReturn(List.of(prevHistory));

        rankService.checkAndPromoteRank(userId, 50);

        // No new history entry
        verify(rankHistoryRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }
}

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    @Mock private AchievementRepository achievementRepository;
    @Mock private UserAchievementRepository userAchievementRepository;
    @Mock private UserGamificationRepository userGamificationRepository;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks private AchievementService achievementService;

    @Test
    void checkXpAchievements_unlocksWhenThresholdMet() {
        UUID userId = UUID.randomUUID();

        Achievement xpHunter = new Achievement();
        xpHunter.setId(UUID.randomUUID());
        xpHunter.setName("XP Hunter");
        xpHunter.setCriteriaType(AchievementCriteriaType.EARN_XP);
        xpHunter.setCriteriaValue(1000L);

        when(achievementRepository.findByCriteriaType(AchievementCriteriaType.EARN_XP))
                .thenReturn(List.of(xpHunter));
        when(userAchievementRepository.existsByUserIdAndAchievementId(userId, xpHunter.getId()))
                .thenReturn(false);

        achievementService.checkXpAchievements(userId, 1500);

        verify(userAchievementRepository).save(any(UserAchievement.class));
        verify(eventPublisher).publish(any(com.riksa.gamification.event.AchievementUnlockedEvent.class));
    }

    @Test
    void checkXpAchievements_skipsAlreadyEarned() {
        UUID userId = UUID.randomUUID();
        UUID achId = UUID.randomUUID();

        Achievement xpHunter = new Achievement();
        xpHunter.setId(achId);
        xpHunter.setName("XP Hunter");
        xpHunter.setCriteriaType(AchievementCriteriaType.EARN_XP);
        xpHunter.setCriteriaValue(1000L);

        when(achievementRepository.findByCriteriaType(AchievementCriteriaType.EARN_XP))
                .thenReturn(List.of(xpHunter));
        when(userAchievementRepository.existsByUserIdAndAchievementId(userId, achId))
                .thenReturn(true); // Already earned

        achievementService.checkXpAchievements(userId, 5000);

        // Should not save again
        verify(userAchievementRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void checkXpAchievements_thresholdNotMet_noUnlock() {
        UUID userId = UUID.randomUUID();
        UUID achId = UUID.randomUUID();

        Achievement xpHunter = new Achievement();
        xpHunter.setId(achId);
        xpHunter.setName("XP Hunter");
        xpHunter.setCriteriaType(AchievementCriteriaType.EARN_XP);
        xpHunter.setCriteriaValue(1000L);

        when(achievementRepository.findByCriteriaType(AchievementCriteriaType.EARN_XP))
                .thenReturn(List.of(xpHunter));
        when(userAchievementRepository.existsByUserIdAndAchievementId(userId, achId))
                .thenReturn(false);

        achievementService.checkXpAchievements(userId, 500); // Only 500, needs 1000

        verify(userAchievementRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }
}