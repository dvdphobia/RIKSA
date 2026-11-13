package com.riksa.report.repository;

import com.riksa.report.entity.ContentReport;
import com.riksa.report.enums.ReportStatus;
import com.riksa.report.enums.ReportTargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContentReportRepository extends JpaRepository<ContentReport, UUID> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(UUID reporterId, ReportTargetType targetType, UUID targetId);

    long countByStatus(ReportStatus status);

    long countByReporterId(UUID reporterId);

    @Modifying
    @Query("DELETE FROM ContentReport r WHERE r.targetType = :targetType AND r.targetId IN :targetIds")
    int deleteByTargetTypeAndTargetIdIn(@Param("targetType") ReportTargetType targetType,
                                        @Param("targetIds") List<UUID> targetIds);

    // The current schema supports POST, COMMENT, and BLOG targets only. This returns
    // reports on owned content; direct user-target reports are not part of the schema.
    @Query(value = """
        SELECT r.*
        FROM content_reports r
        WHERE (r.target_type = 'POST' AND r.target_id IN
                   (SELECT p.id FROM posts p WHERE p.author_id = :userId))
           OR (r.target_type = 'COMMENT' AND r.target_id IN
                   (SELECT c.id FROM comments c WHERE c.author_id = :userId))
           OR (r.target_type = 'BLOG' AND r.target_id IN
                   (SELECT b.id FROM blog_posts b WHERE b.author_id = :userId))
        ORDER BY r.created_at DESC, r.id DESC
        """, nativeQuery = true)
    List<ContentReport> findReportsAgainstUserContent(@Param("userId") UUID userId);

    @Query("""
        SELECT r FROM ContentReport r
        WHERE r.status = :status
          AND r.targetType = COALESCE(:targetType, r.targetType)
        ORDER BY r.createdAt ASC, r.id ASC
    """)
    List<ContentReport> findFirstPage(@Param("status") ReportStatus status,
                                      @Param("targetType") ReportTargetType targetType,
                                      Pageable pageable);

    @Query("""
        SELECT r FROM ContentReport r
        WHERE r.status = :status
          AND r.targetType = COALESCE(:targetType, r.targetType)
          AND (r.createdAt > :cursorCreatedAt
               OR (r.createdAt = :cursorCreatedAt AND r.id > :cursorId))
        ORDER BY r.createdAt ASC, r.id ASC
    """)
    List<ContentReport> findAfterCursor(@Param("status") ReportStatus status,
                                        @Param("targetType") ReportTargetType targetType,
                                        @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
                                        @Param("cursorId") UUID cursorId,
                                        Pageable pageable);
}
