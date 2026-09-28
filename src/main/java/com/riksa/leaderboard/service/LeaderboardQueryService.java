package com.riksa.leaderboard.service;

import com.riksa.common.enums.ScopeType;
import com.riksa.auth.repository.UserRepository;
import com.riksa.leaderboard.entity.LeaderboardSnapshot;
import com.riksa.leaderboard.repository.LeaderboardSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardQueryService {

    private final LeaderboardSnapshotRepository snapshotRepository;
    private final RedisLeaderboardStore redisLeaderboardStore;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<LeaderboardSnapshot> getLeaderboard(ScopeType scope, String scopeId, LocalDate date) {
        if (LocalDate.now().equals(date)) {
            try {
                if (redisLeaderboardStore.isEnabledAndReady()) {
                    return visibleAndRanked(redisLeaderboardStore.getLeaderboard(scope, scopeId));
                }
            } catch (RuntimeException e) {
                log.warn("Redis leaderboard read failed; using PostgreSQL snapshot", e);
            }
        }
        return visibleAndRanked(snapshotRepository.findByScopeAndScopeIdAndSnapshotDateOrderByRankAsc(
                scope, scopeId, date));
    }

    @Transactional(readOnly = true)
    public List<LeaderboardSnapshot> getSnapshotLeaderboard(ScopeType scope, String scopeId, LocalDate date) {
        return visibleAndRanked(snapshotRepository.findByScopeAndScopeIdAndSnapshotDateOrderByRankAsc(
                scope, scopeId, date));
    }

    @Transactional(readOnly = true)
    public Optional<LeaderboardSnapshot> getMyRank(UUID userId, ScopeType scope, String scopeId) {
        return getLeaderboard(scope, scopeId, LocalDate.now()).stream()
                .filter(row -> row.getUserId().equals(userId))
                .findFirst();
    }

    private List<LeaderboardSnapshot> visibleAndRanked(List<LeaderboardSnapshot> snapshots) {
        if (snapshots.isEmpty()) return List.of();

        List<UUID> ids = snapshots.stream().map(LeaderboardSnapshot::getUserId).distinct().toList();
        Set<UUID> eligibleIds = new HashSet<>();
        for (int start = 0; start < ids.size(); start += 1000) {
            int end = Math.min(start + 1000, ids.size());
            eligibleIds.addAll(userRepository.findLeaderboardEligibleIds(
                    ids.subList(start, end), OffsetDateTime.now()));
        }

        List<LeaderboardSnapshot> result = new ArrayList<>();
        int visiblePosition = 0;
        int visibleRank = 0;
        Integer previousScore = null;
        for (LeaderboardSnapshot row : snapshots) {
            if (!eligibleIds.contains(row.getUserId())) continue;
            visiblePosition++;
            if (previousScore == null || previousScore != row.getScore()) {
                visibleRank = visiblePosition;
                previousScore = row.getScore();
            }
            result.add(copyWithRank(row, visibleRank));
        }
        return result;
    }

    private LeaderboardSnapshot copyWithRank(LeaderboardSnapshot source, int rank) {
        LeaderboardSnapshot copy = new LeaderboardSnapshot();
        copy.setId(source.getId());
        copy.setUserId(source.getUserId());
        copy.setScope(source.getScope());
        copy.setScopeId(source.getScopeId());
        copy.setRank(rank);
        copy.setScore(source.getScore());
        copy.setRankingTier(source.getRankingTier());
        copy.setSnapshotDate(source.getSnapshotDate());
        copy.setCreatedAt(source.getCreatedAt());
        return copy;
    }
}
