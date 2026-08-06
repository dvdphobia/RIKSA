package com.riksa.admin.repository;

import com.riksa.admin.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {

    List<AdminAuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, UUID targetId);

    @Query("""
        SELECT a FROM AdminAuditLog a
        WHERE (:targetType IS NULL OR a.targetType = :targetType)
          AND (:targetId IS NULL OR a.targetId = :targetId)
        ORDER BY a.createdAt DESC, a.id DESC
    """)
    Page<AdminAuditLog> search(@Param("targetType") String targetType,
                               @Param("targetId") UUID targetId,
                               Pageable pageable);
}
