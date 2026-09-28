package com.riksa.admin.service;

import com.riksa.admin.dto.XpBoostRequest;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.audit.AuditLogService;
import com.riksa.gamification.entity.Achievement;
import com.riksa.gamification.repository.AchievementRepository;
import com.riksa.gamification.repository.RankTierRepository;
import com.riksa.gamification.repository.StreakMilestoneRepository;
import com.riksa.gamification.repository.UserAchievementRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.repository.XpBoostRepository;
import com.riksa.gamification.repository.XpTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GamificationAdminServiceTest {

    @Mock private AchievementRepository achievementRepository;
    @Mock private UserAchievementRepository userAchievementRepository;
    @Mock private RankTierRepository rankTierRepository;
    @Mock private UserGamificationRepository userGamificationRepository;
    @Mock private StreakMilestoneRepository streakMilestoneRepository;
    @Mock private XpBoostRepository xpBoostRepository;
    @Mock private XpTransactionRepository xpTransactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private GamificationAdminService service;

    @Test
    void deleteAchievement_blocksWhenUsersHaveEarnedIt() {
        UUID achievementId = UUID.randomUUID();
        Achievement achievement = new Achievement();
        achievement.setId(achievementId);
        achievement.setName("First lesson");
        when(achievementRepository.findById(achievementId)).thenReturn(Optional.of(achievement));
        when(userAchievementRepository.countByAchievementId(achievementId)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteAchievement(UUID.randomUUID(), achievementId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been earned");

        verify(achievementRepository, never()).delete(any(Achievement.class));
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void createXpBoost_rejectsOverlappingWindow() {
        UUID actorId = UUID.randomUUID();
        OffsetDateTime startsAt = OffsetDateTime.now().plusDays(1);
        XpBoostRequest request = new XpBoostRequest("Weekend", BigDecimal.valueOf(2),
                startsAt, startsAt.plusDays(2));
        when(xpBoostRepository.existsByStartsAtBeforeAndEndsAtAfter(request.endsAt(), request.startsAt())).thenReturn(true);

        assertThatThrownBy(() -> service.createXpBoost(actorId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlaps");

        verify(xpBoostRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }
}
