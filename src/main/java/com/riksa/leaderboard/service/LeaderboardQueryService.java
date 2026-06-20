package com.riksa.leaderboard.service;

import com.riksa.common.enums.ScopeType;
import com.riksa.leaderboard.entity.LeaderboardSnapshot;
import com.riksa.leaderboard.repository.LeaderboardSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaderboardQueryService {

    private final LeaderboardSnapshotRepository snapshotRepository;

    public List<LeaderboardSnapshot> getLeaderboard(ScopeType scope, String scopeId, LocalDate date) {
        return snapshotRepository.findByScopeAndScopeIdAndSnapshotDateOrderByRankAsc(
                scope, scopeId, date);
    }

    public Optional<LeaderboardSnapshot> getMyRank(UUID userId, ScopeType scope, String scopeId) {
        return snapshotRepository.findByUserIdAndScopeAndScopeIdAndSnapshotDate(
                userId, scope, scopeId, LocalDate.now());
    }
}