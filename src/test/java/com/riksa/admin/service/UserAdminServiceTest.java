package com.riksa.admin.service;

import com.riksa.admin.dto.AdminUserResponse;
import com.riksa.admin.dto.UserOverviewResponse;
import com.riksa.admin.dto.XpAdjustmentRequest;
import com.riksa.admin.dto.SuspensionRequest;
import com.riksa.common.audit.AuditLogService;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.event.EventPublisher;
import com.riksa.common.enums.AchievementRarity;
import com.riksa.common.enums.TierType;
import com.riksa.gamification.entity.Achievement;
import com.riksa.gamification.entity.RankTier;
import com.riksa.gamification.entity.UserAchievement;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.entity.XpTransaction;
import com.riksa.gamification.repository.AchievementRepository;
import com.riksa.gamification.repository.RankTierRepository;
import com.riksa.gamification.repository.UserAchievementRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.repository.XpTransactionRepository;
import com.riksa.gamification.service.XpLedgerService;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.leaderboard.service.RedisLeaderboardProjector;
import com.riksa.report.entity.ContentReport;
import com.riksa.report.enums.ReportReason;
import com.riksa.report.enums.ReportStatus;
import com.riksa.report.enums.ReportTargetType;
import com.riksa.report.repository.ContentReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private UserGamificationRepository userGamificationRepository;
    @Mock private RankTierRepository rankTierRepository;
    @Mock private AchievementRepository achievementRepository;
    @Mock private UserAchievementRepository userAchievementRepository;
    @Mock private XpTransactionRepository xpTransactionRepository;
    @Mock private XpLedgerService xpLedgerService;
    @Mock private LessonCompletionRepository lessonCompletionRepository;
    @Mock private ContentReportRepository contentReportRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private RedisLeaderboardProjector redisLeaderboardProjector;

    @InjectMocks private UserAdminService userAdminService;

    private final UUID actorId = UUID.randomUUID();

    @Test
    void getOverview_assemblesGamificationAchievementsCompletionsAndReports() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("learner");
        user.setDisplayName("Learner");
        user.setEmailVerified(true);
        user.setRole(Role.USER);
        user.setFailedLoginAttempts(3);
        user.setLeaderboardExcluded(true);

        UserGamification gamification = new UserGamification();
        gamification.setUserId(userId);
        gamification.setTotalXp(450);
        gamification.setCurrentStreak(4);
        gamification.setHighestStreak(9);
        gamification.setLastCompletedAt(OffsetDateTime.now().minusHours(2));
        RankTier tier = new RankTier();
        tier.setTierName(TierType.SILVER);
        tier.setIconUrl("silver.png");

        UUID achievementId = UUID.randomUUID();
        Achievement achievement = new Achievement();
        achievement.setId(achievementId);
        achievement.setName("Quick Learner");
        achievement.setIconUrl("quick.png");
        achievement.setRarity(AchievementRarity.RARE);
        achievement.setXpReward(25);
        UserAchievement earned = new UserAchievement();
        earned.setAchievementId(achievementId);
        earned.setUnlockedAt(OffsetDateTime.now().minusDays(1));

        UUID lessonId = UUID.randomUUID();
        LessonCompletionRepository.RecentCompletionRow completion =
                mock(LessonCompletionRepository.RecentCompletionRow.class);
        when(completion.getLessonId()).thenReturn(lessonId);
        when(completion.getLessonTitle()).thenReturn("Fractions");
        when(completion.getXpEarned()).thenReturn(12);
        when(completion.getScore()).thenReturn(95);
        when(completion.getCompletedAt()).thenReturn(OffsetDateTime.now().minusDays(1));

        ContentReport report = new ContentReport();
        report.setId(UUID.randomUUID());
        report.setTargetType(ReportTargetType.POST);
        report.setTargetId(UUID.randomUUID());
        report.setReason(ReportReason.SPAM);
        report.setStatus(ReportStatus.OPEN);
        report.setCreatedAt(OffsetDateTime.now().minusDays(2));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of(earned));
        when(achievementRepository.findAll()).thenReturn(List.of(achievement));
        when(userGamificationRepository.findByUserId(userId)).thenReturn(Optional.of(gamification));
        when(rankTierRepository.findFirstByMinXpLessThanEqualOrderByMinXpDesc(450)).thenReturn(Optional.of(tier));
        when(lessonCompletionRepository.countByUserId(userId)).thenReturn(6L);
        when(lessonCompletionRepository.findRecentByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(List.of(completion));
        when(contentReportRepository.countByReporterId(userId)).thenReturn(2L);
        when(contentReportRepository.findReportsAgainstUserContent(userId)).thenReturn(List.of(report));

        UserOverviewResponse result = userAdminService.getOverview(userId);

        assertThat(result.failedLoginAttempts()).isEqualTo(3);
        assertThat(result.leaderboardExcluded()).isTrue();
        assertThat(result.gamification().totalXp()).isEqualTo(450);
        assertThat(result.gamification().tier()).isEqualTo("SILVER");
        assertThat(result.gamification().tierIconUrl()).isEqualTo("silver.png");
        assertThat(result.achievements()).hasSize(1);
        assertThat(result.achievements().getFirst().earned()).isTrue();
        assertThat(result.lessonsCompleted()).isEqualTo(6);
        assertThat(result.recentCompletions().getFirst().lessonTitle()).isEqualTo("Fractions");
        assertThat(result.reportsFiled()).isEqualTo(2);
        assertThat(result.reportsAgainst().getFirst().targetType()).isEqualTo(ReportTargetType.POST);
    }

    @Test
    void adjustXp_negativeAmountClampsAtZeroAndAuditsAppliedAmount() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("learner");
        user.setRole(Role.USER);
        UserGamification gamification = new UserGamification();
        gamification.setUserId(userId);
        gamification.setTotalXp(7);
        gamification.setXpBalance(2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userGamificationRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(gamification));

        XpAdjustmentRequest request = new XpAdjustmentRequest();
        request.setAmount(-30);
        request.setReason("Correcting an award");

        UserOverviewResponse result = userAdminService.adjustXp(actorId, userId, request);

        assertThat(gamification.getTotalXp()).isZero();
        assertThat(gamification.getXpBalance()).isZero();
        assertThat(result.user().id()).isEqualTo(userId);
        ArgumentCaptor<XpTransaction> transactionCaptor = ArgumentCaptor.forClass(XpTransaction.class);
        verify(xpTransactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getAmount()).isEqualTo(-7);
        assertThat(transactionCaptor.getValue().getRunningBalance()).isZero();
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).record(eq(actorId), eq("USER_XP_ADJUST"), eq("USER"), eq(userId), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue())
                .containsEntry("requestedAmount", -30)
                .containsEntry("appliedAmount", -7)
                .containsEntry("reason", "Correcting an award");
        verify(xpLedgerService, never()).awardXp(any(), anyInt(), any(), any());
    }

    @Test
    void unlock_clearsLockAndResetsFailedAttempts() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("locked");
        user.setRole(Role.USER);
        user.setLockedUntil(OffsetDateTime.now().plusMinutes(15));
        user.setFailedLoginAttempts(5);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserOverviewResponse result = userAdminService.unlock(actorId, userId);

        assertThat(result.user().id()).isEqualTo(userId);
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(auditLogService).record(eq(actorId), eq("USER_UNLOCK"), eq("USER"), eq(userId), anyMap());
    }

    @Test
    void updateRole_validModeratorRole_updatesRole() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("john");
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserResponse result = userAdminService.updateRole(actorId, userId, Role.MODERATOR);

        assertThat(result).isNotNull();
        assertThat(user.getRole()).isEqualTo(Role.MODERATOR);
    }

    @Test
    void updateRole_validAdminRole_updatesRole() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("admin_candidate");
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserResponse result = userAdminService.updateRole(actorId, userId, Role.ADMIN);

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void updateRole_setToUser_throwsException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.MODERATOR);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userAdminService.updateRole(actorId, userId, Role.USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot set role to USER via admin endpoint");
    }

    @Test
    void updateRole_nonExistentUser_throwsException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.updateRole(actorId, userId, Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateRole_recordsAuditEntry() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userAdminService.updateRole(actorId, userId, Role.MODERATOR);

        verify(auditLogService).record(eq(actorId), eq("USER_ROLE_CHANGE"), eq("USER"), eq(userId),
                eq(Map.of("oldRole", "USER", "newRole", "MODERATOR")));
    }

    @Test
    void list_returnsPageWithSuspensionInfo() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("spammer");
        user.setDisplayName("Spammer");
        user.setRole(Role.USER);
        user.setSuspendedUntil(OffsetDateTime.now().plusDays(7));
        user.setSuspensionReason("spam");

        when(userRepository.search(eq("spam"), eq(null), eq(true), any(OffsetDateTime.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<AdminUserResponse> result = userAdminService.list("spam", null, true, 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
        AdminUserResponse row = result.getContent().get(0);
        assertThat(row.username()).isEqualTo("spammer");
        assertThat(row.suspensionReason()).isEqualTo("spam");
        assertThat(row.suspendedUntil()).isEqualTo(user.getSuspendedUntil());
    }

    @Test
    void suspend_validRequest_setsFieldsAndAudits() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime until = OffsetDateTime.now().plusDays(7);
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        SuspensionRequest request = new SuspensionRequest();
        request.setReason("spam");
        request.setUntil(until);

        AdminUserResponse result = userAdminService.suspend(actorId, userId, request);

        assertThat(result).isNotNull();
        assertThat(user.getSuspendedUntil()).isEqualTo(until);
        assertThat(user.getSuspensionReason()).isEqualTo("spam");

        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).record(eq(actorId), eq("USER_SUSPEND"), eq("USER"), eq(userId), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue())
                .containsEntry("reason", "spam")
                .containsEntry("until", until.toString());
    }

    @Test
    void suspend_moderatorTarget_throwsException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.MODERATOR);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        SuspensionRequest request = new SuspensionRequest();
        request.setReason("abuse");
        request.setUntil(OffsetDateTime.now().plusDays(1));

        assertThatThrownBy(() -> userAdminService.suspend(actorId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot suspend staff accounts");

        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void suspend_adminTarget_throwsException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        SuspensionRequest request = new SuspensionRequest();
        request.setReason("abuse");
        request.setUntil(OffsetDateTime.now().plusDays(1));

        assertThatThrownBy(() -> userAdminService.suspend(actorId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot suspend staff accounts");
    }

    @Test
    void suspend_pastUntil_throwsException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        SuspensionRequest request = new SuspensionRequest();
        request.setReason("spam");
        request.setUntil(OffsetDateTime.now().minusDays(1));

        assertThatThrownBy(() -> userAdminService.suspend(actorId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Suspension end date must be in the future");

        assertThat(user.getSuspendedUntil()).isNull();
    }

    @Test
    void suspend_nonExistentUser_throwsException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        SuspensionRequest request = new SuspensionRequest();
        request.setReason("spam");
        request.setUntil(OffsetDateTime.now().plusDays(1));

        assertThatThrownBy(() -> userAdminService.suspend(actorId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void suspend_repeated_overwritesPrevious() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);
        user.setSuspendedUntil(OffsetDateTime.now().plusDays(1));
        user.setSuspensionReason("old reason");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        SuspensionRequest request = new SuspensionRequest();
        request.setReason("new reason");
        request.setUntil(OffsetDateTime.now().plusDays(30));

        userAdminService.suspend(actorId, userId, request);

        assertThat(user.getSuspensionReason()).isEqualTo("new reason");

        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).record(eq(actorId), eq("USER_SUSPEND"), eq("USER"), eq(userId), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue()).containsEntry("previousReason", "old reason");
    }

    @Test
    void unsuspend_suspendedUser_clearsFieldsAndAudits() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime until = OffsetDateTime.now().plusDays(3);
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);
        user.setSuspendedUntil(until);
        user.setSuspensionReason("spam");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserResponse result = userAdminService.unsuspend(actorId, userId);

        assertThat(result).isNotNull();
        assertThat(user.getSuspendedUntil()).isNull();
        assertThat(user.getSuspensionReason()).isNull();

        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).record(eq(actorId), eq("USER_UNSUSPEND"), eq("USER"), eq(userId), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue())
                .containsEntry("previousReason", "spam")
                .containsEntry("previousUntil", until.toString());
    }

    @Test
    void unsuspend_notSuspended_throwsException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userAdminService.unsuspend(actorId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User is not suspended");

        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void unsuspend_expiredSuspension_throwsException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);
        user.setSuspendedUntil(OffsetDateTime.now().minusDays(1));
        user.setSuspensionReason("spam");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userAdminService.unsuspend(actorId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User is not suspended");
    }
}
