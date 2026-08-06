package com.riksa.admin.service;

import com.riksa.admin.dto.AuditLogResponse;
import com.riksa.admin.entity.AdminAuditLog;
import com.riksa.admin.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AdminAuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actorId, String action, String targetType, UUID targetId, Map<String, Object> details) {
        try {
            AdminAuditLog entry = new AdminAuditLog();
            entry.setActorId(actorId);
            entry.setAction(action);
            entry.setTargetType(targetType);
            entry.setTargetId(targetId);
            entry.setDetails(details);
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Failed to write audit log: actor={}, action={}, targetType={}, targetId={}",
                    actorId, action, targetType, targetId, ex);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(String targetType, UUID targetId, int page, int size) {
        return auditLogRepository.search(targetType, targetId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AdminAuditLog entity) {
        return new AuditLogResponse(
                entity.getId(),
                entity.getActorId(),
                entity.getAction(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getDetails(),
                entity.getCreatedAt()
        );
    }
}
