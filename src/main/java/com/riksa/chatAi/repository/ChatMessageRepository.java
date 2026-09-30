package com.riksa.chatAi.repository;

import com.riksa.chatAi.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
    List<ChatMessage> findBySessionIdOrderByCreatedAtAscIdAsc(UUID sessionId);
    List<ChatMessage> findTop20BySessionIdOrderByCreatedAtDesc(UUID sessionId);

    @Query("""
            SELECT COUNT(message) FROM ChatMessage message
            WHERE message.session.user.id = :userId
              AND message.role = 'user'
              AND message.createdAt >= :start
              AND message.createdAt < :end
            """)
    long countUserMessagesByUserIdAndCreatedAtInRange(
            @Param("userId") UUID userId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);

    @Query(value = """
        SELECT cs.user_id AS "userId",
               u.username AS "username",
               u.display_name AS "displayName",
               COUNT(cm.id) AS "messageCount",
               COUNT(DISTINCT cs.id) AS "sessionCount",
               MAX(cm.created_at) AS "lastMessageAt"
        FROM chat_messages cm
        JOIN chat_sessions cs ON cs.id = cm.session_id
        JOIN users u ON u.id = cs.user_id
        WHERE cm.created_at >= :since
        GROUP BY cs.user_id, u.username, u.display_name
        ORDER BY COUNT(cm.id) DESC, MAX(cm.created_at) DESC
        """, nativeQuery = true)
    List<UserUsageRow> findUsageByCreatedAtAfter(@Param("since") OffsetDateTime since);

    interface UserUsageRow {
        UUID getUserId();
        String getUsername();
        String getDisplayName();
        long getMessageCount();
        long getSessionCount();
        OffsetDateTime getLastMessageAt();
    }
}
