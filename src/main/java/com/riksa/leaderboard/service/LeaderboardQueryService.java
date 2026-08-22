package com.riksa.leaderboard.service;

import com.riksa.common.enums.ScopeType;
import com.riksa.leaderboard.entity.LeaderboardSnapshot;
import com.riksa.leaderboard.repository.LeaderboardSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardQueryService {

    private final LeaderboardSnapshotRepository snapshotRepository;
    private final RedisLeaderboardStore redisLeaderboardStore;

    public List<LeaderboardSnapshot> getLeaderboard(ScopeType scope, String scopeId, LocalDate date) {
        if (LocalDate.now().equals(date)) {
            try {
                if (redisLeaderboardStore.isEnabledAndReady()) {
                    return redisLeaderboardStore.getLeaderboard(scope, scopeId);
                }
            } catch (RuntimeException e) {
                log.warn("Redis leaderboard read failed; using PostgreSQL snapshot", e);
            }
        }
        return snapshotRepository.findByScopeAndScopeIdAndSnapshotDateOrderByRankAsc(
                scope, scopeId, date);
    }

    public Optional<LeaderboardSnapshot> getMyRank(UUID userId, ScopeType scope, String scopeId) {
        try {
            if (redisLeaderboardStore.isEnabledAndReady()) {
                return redisLeaderboardStore.getMyRank(userId, scope, scopeId);
            }
        } catch (RuntimeException e) {
            log.warn("Redis rank read failed; using PostgreSQL snapshot", e);
        }
        return snapshotRepository.findByUserIdAndScopeAndScopeIdAndSnapshotDate(
                userId, scope, scopeId, LocalDate.now());
    }
}
