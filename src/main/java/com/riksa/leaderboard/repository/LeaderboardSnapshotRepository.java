package com.riksa.leaderboard.repository;

import com.riksa.common.enums.ScopeType;
import com.riksa.leaderboard.entity.LeaderboardSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaderboardSnapshotRepository extends JpaRepository<LeaderboardSnapshot, UUID> {

    List<LeaderboardSnapshot> findByScopeAndScopeIdAndSnapshotDateOrderByRankAsc(
            ScopeType scope, String scopeId, LocalDate snapshotDate);

    Optional<LeaderboardSnapshot> findByUserIdAndScopeAndScopeIdAndSnapshotDate(
            UUID userId, ScopeType scope, String scopeId, LocalDate snapshotDate);

    List<LeaderboardSnapshot> findBySnapshotDate(LocalDate snapshotDate);

    @Query("SELECT DISTINCT s.snapshotDate FROM LeaderboardSnapshot s ORDER BY s.snapshotDate DESC")
    List<LocalDate> findSnapshotDates();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM LeaderboardSnapshot s WHERE s.snapshotDate = :snapshotDate")
    void deleteBySnapshotDate(LocalDate snapshotDate);
}
