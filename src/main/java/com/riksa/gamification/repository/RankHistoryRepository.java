package com.riksa.gamification.repository;

import com.riksa.gamification.entity.RankHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RankHistoryRepository extends JpaRepository<RankHistory, UUID> {

    List<RankHistory> findByUserIdOrderByChangedAtDesc(UUID userId);
    List<RankHistory> findByUserIdOrderByChangedAtDesc(UUID userId, Pageable pageable);

    /**
     * Cursor-based pagination for rank history.
     * Orders by changedAt DESC, then id DESC for stable pagination.
     */
    @Query("""
        SELECT rh FROM RankHistory rh
        WHERE rh.userId = :userId
          AND (:cursorChangedAt IS NULL OR rh.changedAt < :cursorChangedAt
               OR (rh.changedAt = :cursorChangedAt AND rh.id < :cursorId))
        ORDER BY rh.changedAt DESC, rh.id DESC
    """)
    List<RankHistory> findByUserIdAfterCursor(@Param("userId") UUID userId,
                                               @Param("cursorChangedAt") OffsetDateTime cursorChangedAt,
                                               @Param("cursorId") UUID cursorId,
                                               Pageable pageable);
}
