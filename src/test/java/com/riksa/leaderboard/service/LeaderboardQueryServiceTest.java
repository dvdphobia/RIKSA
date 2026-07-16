package com.riksa.leaderboard.service;

import com.riksa.common.enums.ScopeType;
import com.riksa.leaderboard.entity.LeaderboardSnapshot;
import com.riksa.leaderboard.repository.LeaderboardSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardQueryServiceTest {

    @Mock private LeaderboardSnapshotRepository snapshotRepository;

    @InjectMocks private LeaderboardQueryService leaderboardQueryService;

    @Test
    void getLeaderboard_returnsSortedByRank() {
        LocalDate date = LocalDate.of(2026, 7, 16);
        ScopeType scope = ScopeType.COUNTRY;
        String scopeId = "CAMBODIA";

        LeaderboardSnapshot s1 = new LeaderboardSnapshot();
        s1.setUserId(UUID.randomUUID());
        s1.setRank(1);
        s1.setScore(500);

        LeaderboardSnapshot s2 = new LeaderboardSnapshot();
        s2.setUserId(UUID.randomUUID());
        s2.setRank(2);
        s2.setScore(300);

        when(snapshotRepository.findByScopeAndScopeIdAndSnapshotDateOrderByRankAsc(scope, scopeId, date))
                .thenReturn(List.of(s1, s2));

        List<LeaderboardSnapshot> result = leaderboardQueryService.getLeaderboard(scope, scopeId, date);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRank()).isEqualTo(1);
        assertThat(result.get(1).getRank()).isEqualTo(2);
    }

    @Test
    void getMyRank_existingSnapshot_returnsRank() {
        UUID userId = UUID.randomUUID();
        ScopeType scope = ScopeType.PROVINCE;
        String scopeId = "PHNOM_PENH";

        LeaderboardSnapshot snapshot = new LeaderboardSnapshot();
        snapshot.setUserId(userId);
        snapshot.setRank(5);
        snapshot.setScore(200);

        when(snapshotRepository.findByUserIdAndScopeAndScopeIdAndSnapshotDate(
                eq(userId), eq(scope), eq(scopeId), any(LocalDate.class)))
                .thenReturn(Optional.of(snapshot));

        Optional<LeaderboardSnapshot> result = leaderboardQueryService.getMyRank(userId, scope, scopeId);

        assertThat(result).isPresent();
        assertThat(result.get().getRank()).isEqualTo(5);
    }

    @Test
    void getMyRank_noSnapshot_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        when(snapshotRepository.findByUserIdAndScopeAndScopeIdAndSnapshotDate(
                eq(userId), eq(ScopeType.COUNTRY), eq("CAMBODIA"), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        Optional<LeaderboardSnapshot> result = leaderboardQueryService.getMyRank(
                userId, ScopeType.COUNTRY, "CAMBODIA");

        assertThat(result).isEmpty();
    }
}