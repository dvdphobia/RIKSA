package com.riksa.chatAi.repository;

import com.riksa.chatAi.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    Optional<ChatSession> findByIdAndUserId(UUID id, UUID userId);

    @Query(value = """
        SELECT cs.id AS "id",
               cs.user_id AS "userId",
               cs.context_summary AS "contextSummary",
               COUNT(cm.id) AS "messageCount",
               cs.started_at AS "startedAt",
               cs.updated_at AS "updatedAt"
        FROM chat_sessions cs
        LEFT JOIN chat_messages cm ON cm.session_id = cs.id
        WHERE cs.user_id = :userId
        GROUP BY cs.id, cs.user_id, cs.context_summary, cs.started_at, cs.updated_at
        ORDER BY cs.updated_at DESC, cs.id DESC
        """, nativeQuery = true)
    List<SessionSummaryRow> findSessionSummariesByUserId(@Param("userId") UUID userId);

    interface SessionSummaryRow {
        UUID getId();
        UUID getUserId();
        String getContextSummary();
        long getMessageCount();
        java.time.OffsetDateTime getStartedAt();
        java.time.OffsetDateTime getUpdatedAt();
    }
}
