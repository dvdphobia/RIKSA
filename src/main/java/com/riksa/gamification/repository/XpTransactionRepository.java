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

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM XpTransaction t
        WHERE t.userId = :userId AND t.sourceType = :sourceType
          AND t.createdAt >= :from AND t.createdAt < :until
    """)
    long sumAmountByUserAndSourceTypeInWindow(@Param("userId") UUID userId,
                                               @Param("sourceType") String sourceType,
                                               @Param("from") OffsetDateTime from,
                                               @Param("until") OffsetDateTime until);

    @Query("""
        SELECT t.userId AS userId,
               SUM(t.amount) AS xpGained,
               COUNT(t) AS transactionCount,
               SUM(CASE WHEN t.sourceType = 'LESSON_COMPLETION' THEN 1 ELSE 0 END) AS lessonCompletions,
               MIN(t.createdAt) AS firstAt,
               MAX(t.createdAt) AS lastAt
        FROM XpTransaction t
        WHERE t.createdAt >= :since
          AND t.createdAt <= :until
          AND t.sourceType <> 'ADMIN_ADJUST'
          AND t.amount > 0
        GROUP BY t.userId
        HAVING SUM(t.amount) >= :minXp
        ORDER BY SUM(t.amount) DESC, t.userId ASC
    """)
    List<XpAuditAggregate> findXpAuditAggregates(@Param("since") OffsetDateTime since,
                                                  @Param("until") OffsetDateTime until,
                                                  @Param("minXp") long minXp,
                                                  Pageable pageable);

    interface XpAuditAggregate {
        UUID getUserId();
        long getXpGained();
        long getTransactionCount();
        long getLessonCompletions();
        OffsetDateTime getFirstAt();
        OffsetDateTime getLastAt();
    }

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
