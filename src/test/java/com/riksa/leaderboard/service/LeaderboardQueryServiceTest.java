package com.riksa.leaderboard.service;

import com.riksa.auth.repository.UserRepository;
import com.riksa.common.enums.ScopeType;
import com.riksa.common.enums.TierType;
import com.riksa.leaderboard.entity.LeaderboardSnapshot;
import com.riksa.leaderboard.repository.LeaderboardSnapshotRepository;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardQueryServiceTest {

    @Mock private LeaderboardSnapshotRepository snapshotRepository;
    @Mock private RedisLeaderboardStore redisLeaderboardStore;
    @Mock private UserRepository userRepository;
    @InjectMocks private LeaderboardQueryService leaderboardQueryService;

    @Test
    void getLeaderboard_returnsSortedByRank() {
        LocalDate date = LocalDate.of(2026, 7, 16);
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        LeaderboardSnapshot first = snapshot(firstId, 1, 500, date);
        LeaderboardSnapshot second = snapshot(secondId, 2, 300, date);
        when(snapshotRepository.findByScopeAndScopeIdAndSnapshotDateOrderByRankAsc(
                ScopeType.COUNTRY, "CAMBODIA", date)).thenReturn(List.of(first, second));
        when(userRepository.findLeaderboardEligibleIds(anyCollection(), any()))
                .thenReturn(List.of(firstId, secondId));

        List<LeaderboardSnapshot> result = leaderboardQueryService.getLeaderboard(
                ScopeType.COUNTRY, "CAMBODIA", date);

        assertThat(result).extracting(LeaderboardSnapshot::getUserId).containsExactly(firstId, secondId);
        assertThat(result).extracting(LeaderboardSnapshot::getRank).containsExactly(1, 2);
    }

    @Test
    void getMyRank_existingSnapshot_returnsVisibleRank() {
        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        LeaderboardSnapshot snapshot = snapshot(userId, 5, 200, today);
        when(snapshotRepository.findByScopeAndScopeIdAndSnapshotDateOrderByRankAsc(
                ScopeType.PROVINCE, "PHNOM_PENH", today)).thenReturn(List.of(snapshot));
        when(userRepository.findLeaderboardEligibleIds(anyCollection(), any())).thenReturn(List.of(userId));

        var result = leaderboardQueryService.getMyRank(userId, ScopeType.PROVINCE, "PHNOM_PENH");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getRank()).isEqualTo(1);
    }

    @Test
    void getMyRank_noSnapshot_returnsEmpty() {
        var result = leaderboardQueryService.getMyRank(UUID.randomUUID(), ScopeType.COUNTRY, "CAMBODIA");

        assertThat(result).isEmpty();
    }

    @Test
    void currentLeaderboard_whenRedisReady_readsRedis() {
        UUID userId = UUID.randomUUID();
        LeaderboardSnapshot snapshot = snapshot(userId, 1, 400, LocalDate.now());
        when(redisLeaderboardStore.isEnabledAndReady()).thenReturn(true);
        when(redisLeaderboardStore.getLeaderboard(ScopeType.COUNTRY, "CAMBODIA"))
                .thenReturn(List.of(snapshot));
        when(userRepository.findLeaderboardEligibleIds(anyCollection(), any())).thenReturn(List.of(userId));

        var result = leaderboardQueryService.getLeaderboard(
                ScopeType.COUNTRY, "CAMBODIA", LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUserId()).isEqualTo(userId);
        verifyNoInteractions(snapshotRepository);
    }

    @Test
    void getLeaderboard_filtersExcludedUsersAndRecalculatesVisibleRanks() {
        UUID hiddenId = UUID.randomUUID();
        UUID visibleId = UUID.randomUUID();
        LocalDate date = LocalDate.now().minusDays(1);
        when(snapshotRepository.findByScopeAndScopeIdAndSnapshotDateOrderByRankAsc(
                ScopeType.COUNTRY, "CAMBODIA", date))
                .thenReturn(List.of(snapshot(hiddenId, 1, 900, date), snapshot(visibleId, 2, 800, date)));
        when(userRepository.findLeaderboardEligibleIds(anyCollection(), any()))
                .thenReturn(List.of(visibleId));

        List<LeaderboardSnapshot> result = leaderboardQueryService.getLeaderboard(ScopeType.COUNTRY, "CAMBODIA", date);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUserId()).isEqualTo(visibleId);
        assertThat(result.getFirst().getRank()).isEqualTo(1);
    }

    private LeaderboardSnapshot snapshot(UUID userId, int rank, int score, LocalDate date) {
        LeaderboardSnapshot snapshot = new LeaderboardSnapshot();
        snapshot.setUserId(userId);
        snapshot.setRank(rank);
        snapshot.setScore(score);
        snapshot.setScope(ScopeType.COUNTRY);
        snapshot.setScopeId("CAMBODIA");
        snapshot.setRankingTier(TierType.BRONZE);
        snapshot.setSnapshotDate(date);
        return snapshot;
    }
}
