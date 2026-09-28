package com.riksa.gamification.service;

import com.riksa.admin.dto.settings.XpRulesSettings;
import com.riksa.admin.service.SettingsService;
import com.riksa.common.event.EventPublisher;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.entity.XpBoost;
import com.riksa.gamification.entity.XpTransaction;
import com.riksa.gamification.event.XpGainedEvent;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.repository.XpBoostRepository;
import com.riksa.gamification.repository.XpTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XpLedgerBoostServiceTest {

    @Mock private UserGamificationRepository userGamificationRepository;
    @Mock private XpTransactionRepository xpTransactionRepository;
    @Mock private XpBoostRepository xpBoostRepository;
    @Mock private SettingsService settingsService;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks private XpLedgerService xpLedgerService;

    private UUID userId;
    private UserGamification gamification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        gamification = new UserGamification();
        gamification.setUserId(userId);
        when(userGamificationRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(gamification));
        when(xpTransactionRepository.findByUserIdAndSourceTypeAndSourceId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(userGamificationRepository.save(any(UserGamification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(xpTransactionRepository.save(any(XpTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void awardXp_appliesActiveBoostToGameplaySource() {
        when(xpBoostRepository.findFirstByStartsAtLessThanEqualAndEndsAtGreaterThanOrderByStartsAtDesc(
                any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(Optional.of(boost("2.0")));
        when(settingsService.getXpRules()).thenReturn(new XpRulesSettings(0));

        long total = xpLedgerService.awardXp(userId, 20, "LESSON_COMPLETION", UUID.randomUUID());

        assertThat(total).isEqualTo(40);
        ArgumentCaptor<XpTransaction> transaction = ArgumentCaptor.forClass(XpTransaction.class);
        verify(xpTransactionRepository).save(transaction.capture());
        assertThat(transaction.getValue().getAmount()).isEqualTo(40);
        ArgumentCaptor<XpGainedEvent> event = ArgumentCaptor.forClass(XpGainedEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().getAmount()).isEqualTo(40);
    }

    @Test
    void awardXp_doesNotApplyBoostToAdminAdjustments() {
        long total = xpLedgerService.awardXp(userId, 20, "ADMIN_ADJUST", UUID.randomUUID());

        assertThat(total).isEqualTo(20);
        ArgumentCaptor<XpTransaction> transaction = ArgumentCaptor.forClass(XpTransaction.class);
        verify(xpTransactionRepository).save(transaction.capture());
        assertThat(transaction.getValue().getAmount()).isEqualTo(20);
    }

    @Test
    void awardXp_appliesBoostBeforeClampingToDailyLessonCap() {
        when(xpBoostRepository.findFirstByStartsAtLessThanEqualAndEndsAtGreaterThanOrderByStartsAtDesc(
                any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(Optional.of(boost("2.0")));
        when(settingsService.getXpRules()).thenReturn(new XpRulesSettings(30));
        when(xpTransactionRepository.sumAmountByUserAndSourceTypeInWindow(
                eq(userId), eq("LESSON_COMPLETION"), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(25L);
        gamification.setTotalXp(25);
        gamification.setXpBalance(25);

        long total = xpLedgerService.awardXp(userId, 20, "LESSON_COMPLETION", UUID.randomUUID());

        assertThat(total).isEqualTo(30);
        ArgumentCaptor<XpTransaction> transaction = ArgumentCaptor.forClass(XpTransaction.class);
        verify(xpTransactionRepository).save(transaction.capture());
        assertThat(transaction.getValue().getAmount()).isEqualTo(5);
    }

    @Test
    void awardXp_doesNotApplyBoostToMarketSpend() {
        long total = xpLedgerService.awardXp(userId, 10, "MARKET_SPEND", UUID.randomUUID());

        assertThat(total).isEqualTo(10);
        verify(xpBoostRepository, never()).findFirstByStartsAtLessThanEqualAndEndsAtGreaterThanOrderByStartsAtDesc(
                any(OffsetDateTime.class), any(OffsetDateTime.class));
    }

    private XpBoost boost(String multiplier) {
        XpBoost boost = new XpBoost();
        boost.setMultiplier(new BigDecimal(multiplier));
        return boost;
    }
}
