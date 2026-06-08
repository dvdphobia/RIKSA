package com.riksa.gamification.repository;

import com.riksa.gamification.entity.XpTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface XpTransactionRepository extends JpaRepository<XpTransaction, UUID> {

    Page<XpTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
        SELECT t FROM XpTransaction t
        WHERE t.userId = :userId
          AND (:cursorCreatedAt IS NULL OR t.createdAt < :cursorCreatedAt
               OR (t.createdAt = :cursorCreatedAt AND t.id < :cursorId))
        ORDER BY t.createdAt DESC, t.id DESC
    """)
    List<XpTransaction> findByUserIdAfterCursor(@Param("userId") UUID userId,
                                                 @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
                                                 @Param("cursorId") UUID cursorId,
                                                 Pageable pageable);
}
