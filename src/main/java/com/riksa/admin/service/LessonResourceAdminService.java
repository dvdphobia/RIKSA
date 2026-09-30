package com.riksa.admin.service;

import com.riksa.admin.dto.AdminLessonResource;
import com.riksa.admin.dto.AdminLessonResourceStatusRequest;
import com.riksa.common.audit.AuditLogService;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.resource.entity.LessonResource;
import com.riksa.resource.entity.ResourceKind;
import com.riksa.resource.repository.LessonResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonResourceAdminService {

    private final LessonResourceRepository resourceRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<AdminLessonResource> list(UUID lessonId, Boolean hidden, String query, int page, int size) {
        String safeQuery = query == null || query.isBlank() ? "" : query.trim();
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        return resourceRepository.searchAdmin(lessonId, hidden, safeQuery, PageRequest.of(safePage, safeSize))
                .map(this::toResponse);
    }

    @Transactional
    public AdminLessonResource updateStatus(UUID actorId, UUID id, AdminLessonResourceStatusRequest request) {
        LessonResource resource = getResource(id);
        boolean hidden = request.getHidden();
        if (hidden) {
            resource.setHiddenAt(OffsetDateTime.now());
            resource.setHiddenBy(actorId);
        } else {
            resource.setHiddenAt(null);
            resource.setHiddenBy(null);
        }
        resourceRepository.saveAndFlush(resource);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("hidden", hidden);
        if (request.getReason() != null && !request.getReason().isBlank()) {
            details.put("reason", request.getReason().trim());
        }
        auditLogService.record(actorId, hidden ? "RESOURCE_HIDE" : "RESOURCE_UNHIDE",
                "LESSON_RESOURCE", id, details);
        return resourceRepository.findAdminById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
    }

    @Transactional
    public void delete(UUID actorId, UUID id) {
        LessonResource resource = getResource(id);
        Map<String, Object> details = Map.of(
                "lessonId", resource.getLessonId().toString(),
                "authorId", resource.getAuthorId().toString(),
                "title", resource.getTitle());

        jdbcTemplate.update("DELETE FROM lesson_resource_save_reward WHERE resource_id = ?", id);
        jdbcTemplate.update("DELETE FROM lesson_resource_save WHERE resource_id = ?", id);
        resourceRepository.delete(resource);
        auditLogService.record(actorId, "RESOURCE_DELETE", "LESSON_RESOURCE", id, details);
    }

    private LessonResource getResource(UUID id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
    }

    private AdminLessonResource toResponse(LessonResourceRepository.AdminLessonResourceProjection row) {
        return new AdminLessonResource(
                row.getId(), row.getLessonId(), row.getLessonTitle(), row.getAuthorId(), row.getAuthorUsername(),
                row.getTitle(), row.getUrl(), ResourceKind.valueOf(row.getKind()), row.getHidden(), row.getHiddenAt(),
                row.getHiddenBy(), row.getSaveCount(), row.getCreatedAt());
    }
}
