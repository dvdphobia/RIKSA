package com.riksa.resource.repository;

import com.riksa.resource.entity.LessonResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LessonResourceRepository extends JpaRepository<LessonResource, UUID> {
    String ADMIN_SELECT = """
            SELECT r.id AS \"id\",
                   r.lesson_id AS \"lessonId\",
                   l.title AS \"lessonTitle\",
                   r.author_id AS \"authorId\",
                   u.username AS \"authorUsername\",
                   r.title AS \"title\",
                   r.url AS \"url\",
                   r.kind AS \"kind\",
                   (r.hidden_at IS NOT NULL) AS \"hidden\",
                   r.hidden_at AS \"hiddenAt\",
                   r.hidden_by AS \"hiddenBy\",
                   (SELECT COUNT(*) FROM lesson_resource_save s WHERE s.resource_id = r.id) AS \"saveCount\",
                   r.created_at AS \"createdAt\"
            FROM lesson_resource r
            JOIN lessons l ON l.id = r.lesson_id
            JOIN users u ON u.id = r.author_id
            """;

    long countByAuthorIdAndCreatedAtGreaterThanEqual(UUID authorId, OffsetDateTime startOfDay);

    @Query(value = ADMIN_SELECT + """
            WHERE (CAST(:lessonId AS UUID) IS NULL OR r.lesson_id = CAST(:lessonId AS UUID))
              AND (CAST(:hidden AS BOOLEAN) IS NULL
                   OR (r.hidden_at IS NOT NULL) = CAST(:hidden AS BOOLEAN))
              AND (CAST(:query AS TEXT) = ''
                   OR LOWER(r.title) LIKE CONCAT('%', LOWER(CAST(:query AS TEXT)), '%')
                   OR LOWER(r.url) LIKE CONCAT('%', LOWER(CAST(:query AS TEXT)), '%'))
            ORDER BY r.created_at DESC, r.id DESC
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM lesson_resource r
                    WHERE (CAST(:lessonId AS UUID) IS NULL OR r.lesson_id = CAST(:lessonId AS UUID))
                      AND (CAST(:hidden AS BOOLEAN) IS NULL
                           OR (r.hidden_at IS NOT NULL) = CAST(:hidden AS BOOLEAN))
                      AND (CAST(:query AS TEXT) = ''
                           OR LOWER(r.title) LIKE CONCAT('%', LOWER(CAST(:query AS TEXT)), '%')
                           OR LOWER(r.url) LIKE CONCAT('%', LOWER(CAST(:query AS TEXT)), '%'))
                    """,
            nativeQuery = true)
    Page<AdminLessonResourceProjection> searchAdmin(
            @Param("lessonId") UUID lessonId,
            @Param("hidden") Boolean hidden,
            @Param("query") String query,
            Pageable pageable);

    @Query(value = ADMIN_SELECT + " WHERE r.id = :id", nativeQuery = true)
    java.util.Optional<AdminLessonResourceProjection> findAdminById(@Param("id") UUID id);

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

    interface AdminLessonResourceProjection {
        UUID getId();
        UUID getLessonId();
        String getLessonTitle();
        UUID getAuthorId();
        String getAuthorUsername();
        String getTitle();
        String getUrl();
        String getKind();
        boolean getHidden();
        OffsetDateTime getHiddenAt();
        UUID getHiddenBy();
        long getSaveCount();
        OffsetDateTime getCreatedAt();
    }
}
