package com.riksa.quest.repository;

import com.riksa.quest.entity.QuestLessonProgressEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface QuestLessonProgressEventRepository extends JpaRepository<QuestLessonProgressEvent, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO quest_lesson_progress_events (id, user_id, lesson_id, completed_at)
            VALUES (:id, :userId, :lessonId, :completedAt)
            ON CONFLICT (user_id, lesson_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("userId") UUID userId,
                       @Param("lessonId") UUID lessonId,
                       @Param("completedAt") OffsetDateTime completedAt);
}
