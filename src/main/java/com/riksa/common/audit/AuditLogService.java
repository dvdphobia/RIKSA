package com.riksa.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public Page<AuditLogResponse> search(String targetType, UUID targetId, UUID actorId, String action, int page, int size) {
        AdminAuditLog probe = new AdminAuditLog();
        probe.setTargetType(targetType);
        probe.setTargetId(targetId);
        probe.setActorId(actorId);
        probe.setAction(action);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return auditLogRepository.findAll(Example.of(probe), pageable)
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
