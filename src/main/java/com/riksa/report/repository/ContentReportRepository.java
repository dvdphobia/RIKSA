package com.riksa.report.repository;

import com.riksa.report.entity.ContentReport;
import com.riksa.report.enums.ReportStatus;
import com.riksa.report.enums.ReportTargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
