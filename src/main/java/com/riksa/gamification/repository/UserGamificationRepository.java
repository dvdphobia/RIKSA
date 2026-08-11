package com.riksa.gamification.repository;

import com.riksa.gamification.entity.UserGamification;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGamificationRepository extends JpaRepository<UserGamification, UUID> {

    Optional<UserGamification> findByUserId(UUID userId);

    List<UserGamification> findAllByOrderByTotalXpDesc(Pageable pageable);

    @Query("""
        SELECT ug FROM UserGamification ug
        WHERE (ug.totalXp < :cursorXp
               OR (ug.totalXp = :cursorXp AND ug.userId > :cursorUserId))
        ORDER BY ug.totalXp DESC, ug.userId ASC
    """)
    List<UserGamification> findLeaderboardAfterCursor(@Param("cursorXp") Long cursorXp,
                                                       @Param("cursorUserId") UUID cursorUserId,
                                                       Pageable pageable);

    // Active streak users (e.g. for "users on fire" widgets).
    List<UserGamification> findByCurrentStreakGreaterThan(int streak);

    // Streak-reset job: users whose last qualifying completion is older than the cutoff.
    List<UserGamification> findByLastCompletedAtBefore(OffsetDateTime cutoff);

    // Pessimistic lookup for the hot path (XP grant / streak bump). Callers MUST
    // already be inside a @Transactional method; the lock is released at commit/rollback.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ug from UserGamification ug where ug.userId = :userId")
    Optional<UserGamification> findByIdForUpdate(@Param("userId") UUID userId);

    // Optimistic alternative for low-contention callers (e.g. profile reads that
    // also touch derived fields). The entity's @Version handles conflict detection.
    @Lock(LockModeType.OPTIMISTIC)
    @Query("select ug from UserGamification ug where ug.userId = :userId")
    Optional<UserGamification> findByIdOptimistic(@Param("userId") UUID userId);

    boolean existsByUserId(UUID userId);

    @Query("""
        SELECT CASE
                 WHEN ug.currentStreak = 0 THEN '0'
                 WHEN ug.currentStreak BETWEEN 1 AND 2 THEN '1-2'
                 WHEN ug.currentStreak BETWEEN 3 AND 6 THEN '3-6'
                 WHEN ug.currentStreak BETWEEN 7 AND 13 THEN '7-13'
                 WHEN ug.currentStreak BETWEEN 14 AND 29 THEN '14-29'
                 ELSE '30+'
               END AS bucket,
               COUNT(ug) AS userCount
        FROM UserGamification ug
        GROUP BY bucket
    """)
    List<StreakBucketCount> findStreakHistogram();

    interface StreakBucketCount {
        String getBucket();
        long getUserCount();
    }
}