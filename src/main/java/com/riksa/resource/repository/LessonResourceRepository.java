package com.riksa.resource.repository;

import com.riksa.resource.entity.LessonResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LessonResourceRepository extends JpaRepository<LessonResource, UUID> {
    long countByAuthorIdAndCreatedAtGreaterThanEqual(UUID authorId, OffsetDateTime startOfDay);

    @Query(value = """
        SELECT r.* FROM lesson_resource r
        LEFT JOIN lesson_resource_save s ON s.resource_id = r.id
        WHERE r.lesson_id = :lessonId AND r.hidden_at IS NULL
        GROUP BY r.id
        ORDER BY COUNT(s.id) DESC, r.created_at DESC, r.id DESC
        """, nativeQuery = true)
    List<LessonResource> findFirstPage(@Param("lessonId") UUID lessonId, Pageable pageable);

    @Query(value = """
        SELECT r.* FROM lesson_resource r
        LEFT JOIN lesson_resource_save s ON s.resource_id = r.id
        WHERE r.lesson_id = :lessonId AND r.hidden_at IS NULL
        GROUP BY r.id
        HAVING COUNT(s.id) < :cursorSaveCount
            OR (COUNT(s.id) = :cursorSaveCount AND
                (r.created_at < :cursorCreatedAt OR
                 (r.created_at = :cursorCreatedAt AND r.id < :cursorId)))
        ORDER BY COUNT(s.id) DESC, r.created_at DESC, r.id DESC
        """, nativeQuery = true)
    List<LessonResource> findAfterCursor(
            @Param("lessonId") UUID lessonId,
            @Param("cursorSaveCount") long cursorSaveCount,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);
}
