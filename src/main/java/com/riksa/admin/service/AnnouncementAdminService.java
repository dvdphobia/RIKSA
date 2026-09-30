package com.riksa.admin.service;

import com.riksa.admin.dto.ActiveAnnouncementResponse;
import com.riksa.admin.dto.AnnouncementRequest;
import com.riksa.admin.dto.AnnouncementResponse;
import com.riksa.admin.entity.Announcement;
import com.riksa.admin.repository.AnnouncementRepository;
import com.riksa.common.audit.AuditLogService;
import com.riksa.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnouncementAdminService {

    private final AnnouncementRepository announcementRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> list() {
        OffsetDateTime now = OffsetDateTime.now();
        return announcementRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(announcement -> toAdminResponse(announcement, now))
                .toList();
    }

    @Transactional
    public AnnouncementResponse create(UUID actorId, AnnouncementRequest request) {
        validateWindow(request);
        Announcement announcement = new Announcement();
        copyRequest(request, announcement);
        announcement = announcementRepository.save(announcement);
        auditLogService.record(actorId, "ANNOUNCEMENT_CREATE", "ANNOUNCEMENT", announcement.getId(),
                details(request));
        return toAdminResponse(announcement, OffsetDateTime.now());
    }

    @Transactional
    public AnnouncementResponse update(UUID actorId, UUID id, AnnouncementRequest request) {
        validateWindow(request);
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + id));
        copyRequest(request, announcement);
        announcement = announcementRepository.save(announcement);
        auditLogService.record(actorId, "ANNOUNCEMENT_UPDATE", "ANNOUNCEMENT", id, details(request));
        return toAdminResponse(announcement, OffsetDateTime.now());
    }

    @Transactional
    public void delete(UUID actorId, UUID id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + id));
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("title", announcement.getTitle());
        details.put("body", announcement.getBody());
        details.put("startsAt", announcement.getStartsAt());
        details.put("endsAt", announcement.getEndsAt());
        auditLogService.record(actorId, "ANNOUNCEMENT_DELETE", "ANNOUNCEMENT", id, details);
        announcementRepository.delete(announcement);
    }

    @Transactional(readOnly = true)
    public List<ActiveAnnouncementResponse> active() {
        return announcementRepository.findActiveAt(OffsetDateTime.now()).stream()
                .map(this::toActiveResponse)
                .toList();
    }

    private void validateWindow(AnnouncementRequest request) {
        if (request.startsAt() == null) {
            throw new IllegalArgumentException("startsAt is required");
        }
        if (request.endsAt() != null && !request.endsAt().isAfter(request.startsAt())) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
    }

    private void copyRequest(AnnouncementRequest request, Announcement announcement) {
        announcement.setTitle(request.title().trim());
        announcement.setBody(request.body().trim());
        announcement.setStartsAt(request.startsAt());
        announcement.setEndsAt(request.endsAt());
    }

    private Map<String, Object> details(AnnouncementRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("title", request.title());
        details.put("body", request.body());
        details.put("startsAt", request.startsAt());
        details.put("endsAt", request.endsAt());
        return details;
    }

    private AnnouncementResponse toAdminResponse(Announcement announcement, OffsetDateTime now) {
        return new AnnouncementResponse(announcement.getId(), announcement.getTitle(), announcement.getBody(),
                announcement.getStartsAt(), announcement.getEndsAt(), status(announcement, now),
                announcement.getUpdatedAt());
    }

    private ActiveAnnouncementResponse toActiveResponse(Announcement announcement) {
        return new ActiveAnnouncementResponse(announcement.getId(), announcement.getTitle(), announcement.getBody(),
                announcement.getStartsAt(), announcement.getEndsAt());
    }

    private String status(Announcement announcement, OffsetDateTime now) {
        if (announcement.getStartsAt().isAfter(now)) {
            return "SCHEDULED";
        }
        if (announcement.getEndsAt() != null && !announcement.getEndsAt().isAfter(now)) {
            return "ENDED";
        }
        return "LIVE";
    }
}
