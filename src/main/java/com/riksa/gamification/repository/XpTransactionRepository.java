package com.riksa.gamification.repository;

import com.riksa.gamification.entity.XpTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface XpTransactionRepository extends JpaRepository<XpTransaction, UUID> {

    Optional<XpTransaction> findByUserIdAndSourceTypeAndSourceId(
            UUID userId, String sourceType, UUID sourceId);

    Page<XpTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
        SELECT t FROM XpTransaction t
        WHERE t.userId = :userId
          AND (t.createdAt < :cursorCreatedAt
               OR (t.createdAt = :cursorCreatedAt AND t.id < :cursorId))
        ORDER BY t.createdAt DESC, t.id DESC
    """)
    List<XpTransaction> findByUserIdAfterCursor(@Param("userId") UUID userId,
                                                 @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
                                                 @Param("cursorId") UUID cursorId,
                                                 Pageable pageable);

    @Query("""
        SELECT t.sourceType AS sourceType,
               CAST(t.createdAt AS LocalDate) AS day,
               SUM(t.amount) AS totalXp,
               COUNT(t) AS transactionCount
        FROM XpTransaction t
        WHERE t.createdAt >= :since
        GROUP BY t.sourceType, CAST(t.createdAt AS LocalDate)
        ORDER BY day DESC, t.sourceType ASC
    """)
    List<DailyXpBySource> findDailyXpBySourceSince(@Param("since") OffsetDateTime since);

    interface DailyXpBySource {
        String getSourceType();
        LocalDate getDay();
        long getTotalXp();
        long getTransactionCount();
    }
}
