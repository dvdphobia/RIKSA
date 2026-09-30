package com.riksa.admin.service;

import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.ProvinceRepository;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.audit.AuditLogService;
import com.riksa.common.enums.ScopeType;
import com.riksa.common.enums.TierType;
import com.riksa.common.event.EventPublisher;
import com.riksa.leaderboard.entity.LeaderboardSnapshot;
import com.riksa.leaderboard.repository.LeaderboardSnapshotRepository;
import com.riksa.leaderboard.service.LeaderboardQueryService;
import com.riksa.leaderboard.service.LeaderboardSnapshotService;
import com.riksa.leaderboard.service.RedisLeaderboardStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardAdminServiceTest {

    @Mock private LeaderboardSnapshotRepository snapshotRepository;
    @Mock private LeaderboardQueryService leaderboardQueryService;
    @Mock private LeaderboardSnapshotService snapshotService;
    @Mock private RedisLeaderboardStore redisLeaderboardStore;
    @Mock private UserRepository userRepository;
    @Mock private ProvinceRepository provinceRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private EventPublisher eventPublisher;
    @InjectMocks private LeaderboardAdminService leaderboardAdminService;

    @Test
    void boardReturnsPanelShapeAndProfileNames() {
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 1);
        User user = new User();
        user.setId(userId);
        user.setUsername("learner");
        user.setDisplayName("Learner Name");
        user.setRole(Role.USER);
        LeaderboardSnapshot snapshot = new LeaderboardSnapshot();
        snapshot.setUserId(userId);
        snapshot.setRank(1);
        snapshot.setScore(1200);
        snapshot.setRankingTier(TierType.SILVER);
        snapshot.setScope(ScopeType.COUNTRY);
        snapshot.setScopeId("CAMBODIA");
        snapshot.setSnapshotDate(date);
        when(leaderboardQueryService.getSnapshotLeaderboard(ScopeType.COUNTRY, "CAMBODIA", date))
                .thenReturn(List.of(snapshot));
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of(user));

        var result = leaderboardAdminService.board(date, ScopeType.COUNTRY, "CAMBODIA", 0, 50);

        assertThat(result.date()).isEqualTo(date);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().username()).isEqualTo("learner");
        assertThat(result.rows().getFirst().tier()).isEqualTo("SILVER");
    }

    @Test
    void refreshRebuildsRedisAndRecordsAudit() {
        UUID actorId = UUID.randomUUID();
        when(snapshotService.refreshSnapshots()).thenReturn(new LeaderboardSnapshotService.SnapshotCounts(4, 9));
        when(redisLeaderboardStore.isEnabled()).thenReturn(true);
        when(snapshotService.loadSnapshots(any(LocalDate.class))).thenReturn(List.of());

        var result = leaderboardAdminService.refresh(actorId);

        assertThat(result.countryRows()).isEqualTo(4);
        assertThat(result.provinceRows()).isEqualTo(9);
        assertThat(result.redisRebuilt()).isTrue();
        verify(redisLeaderboardStore).replaceAll(List.of());
        verify(auditLogService).record(any(), org.mockito.ArgumentMatchers.eq("LEADERBOARD_REFRESH"),
                org.mockito.ArgumentMatchers.eq("LEADERBOARD"), org.mockito.ArgumentMatchers.isNull(), any());
    }
}
