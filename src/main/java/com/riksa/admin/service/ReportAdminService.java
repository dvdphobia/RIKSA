package com.riksa.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riksa.admin.dto.ReportResolutionRequest;
import com.riksa.common.audit.AuditLogService;
import com.riksa.common.exception.InvalidCursorException;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.gamification.dto.CursorPage;
import com.riksa.report.dto.ReportResponse;
import com.riksa.report.entity.ContentReport;
import com.riksa.report.enums.ReportAction;
import com.riksa.report.enums.ReportStatus;
import com.riksa.report.enums.ReportTargetType;
import com.riksa.report.repository.ContentReportRepository;
import com.riksa.report.service.ReportService;
import com.riksa.resource.entity.LessonResource;
import com.riksa.resource.repository.LessonResourceRepository;
import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.enums.ContentStatus;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportAdminService {

    private final ContentReportRepository reportRepository;
    private final PostRepository postRepository;
    private final LessonResourceRepository lessonResourceRepository;
    private final CommentRepository commentRepository;
    private final SocialModerationService socialModerationService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private record ReportCursor(OffsetDateTime t, UUID i) {}

    @Transactional(readOnly = true)
    public CursorPage<ReportResponse> list(String statusStr, String targetTypeStr, String cursorStr, int limit) {
        limit = Math.min(Math.max(limit, 1), 50);

        ReportStatus status = parseStatus(statusStr != null ? statusStr : "OPEN");
        ReportTargetType targetType = targetTypeStr != null
                ? ReportService.parseTargetType(targetTypeStr)
                : null;

        ReportCursor cursor = decodeCursor(cursorStr);
        OffsetDateTime cursorCreatedAt = cursor != null ? cursor.t() : null;
        UUID cursorId = cursor != null ? cursor.i() : null;

        List<ContentReport> rows = cursor == null
                ? reportRepository.findFirstPage(status, targetType, PageRequest.of(0, limit + 1))
                : reportRepository.findAfterCursor(status, targetType, cursorCreatedAt, cursorId,
                        PageRequest.of(0, limit + 1));

        List<ReportResponse> edges = rows.stream()
                .limit(limit)
                .map(ReportService::toResponse)
                .toList();

        boolean hasNext = rows.size() > limit;
        String endCursor = null;
        if (hasNext) {
            ContentReport last = rows.get(limit - 1);
            endCursor = encodeCursor(new ReportCursor(last.getCreatedAt(), last.getId()));
        }

        return new CursorPage<>(edges, hasNext, endCursor);
    }

    @Transactional(readOnly = true)
    public ReportResponse get(UUID reportId) {
        return reportRepository.findById(reportId)
                .map(ReportService::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));
    }

    @Transactional
    public ReportResponse resolve(UUID moderatorId, UUID reportId, ReportResolutionRequest request) {
        ContentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));

        if (report.getStatus() != ReportStatus.OPEN) {
            throw new IllegalArgumentException("Report is already " + report.getStatus());
        }

        ReportStatus newStatus = request.getStatus();
        if (newStatus == ReportStatus.OPEN) {
            throw new IllegalArgumentException("Cannot set status to OPEN via resolution endpoint");
        }

        ReportAction action = request.getAction() != null ? request.getAction() : ReportAction.NONE;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("targetType", report.getTargetType().name());
        details.put("targetId", report.getTargetId().toString());
        details.put("action", action.name());

        if (newStatus == ReportStatus.RESOLVED && action == ReportAction.HIDE) {
            String hideOutcome = hideTarget(report, moderatorId);
            if (hideOutcome != null) {
                details.put("hide", hideOutcome);
            }
        }

        report.setStatus(newStatus);
        report.setResolvedBy(moderatorId);
        report.setResolvedAt(OffsetDateTime.now());
        reportRepository.save(report);

        auditLogService.record(moderatorId, "REPORT_" + newStatus.name(), "REPORT", reportId, details);

        return ReportService.toResponse(report);
    }

    private String hideTarget(ContentReport report, UUID moderatorId) {
        String reason = "Reported as " + report.getReason().name()
                + (report.getDetails() != null && !report.getDetails().isBlank()
                        ? ": " + report.getDetails() : "");

        return switch (report.getTargetType()) {
            case POST -> {
                Post post = postRepository.findById(report.getTargetId()).orElse(null);
                if (post == null) {
                    yield "targetMissing";
                }
                if (post.getStatus() == ContentStatus.HIDDEN) {
                    yield "alreadyHidden";
                }
                socialModerationService.setPostStatus(moderatorId, report.getTargetId(), ContentStatus.HIDDEN, reason);
                yield null;
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(report.getTargetId()).orElse(null);
                if (comment == null) {
                    yield "targetMissing";
                }
                if (comment.getStatus() == ContentStatus.HIDDEN) {
                    yield "alreadyHidden";
                }
                socialModerationService.setCommentStatus(moderatorId, report.getTargetId(), ContentStatus.HIDDEN, reason);
                yield null;
            }
            case BLOG -> throw new IllegalArgumentException(
                    "HIDE action is not supported for BLOG targets; use blog moderation");
            case RESOURCE -> {
                LessonResource resource = lessonResourceRepository.findById(report.getTargetId()).orElse(null);
                if (resource == null) yield "targetMissing";
                if (resource.getHiddenAt() != null) yield "alreadyHidden";
                resource.setHiddenAt(OffsetDateTime.now());
                resource.setHiddenBy(moderatorId);
                lessonResourceRepository.save(resource);
                yield null;
            }
        };
    }

    private ReportStatus parseStatus(String value) {
        try {
            return ReportStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Invalid status: " + value + ". Valid values: OPEN, RESOLVED, DISMISSED");
        }
    }

    private String encodeCursor(ReportCursor cursor) {
        try {
            return java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(cursor));
        } catch (JsonProcessingException e) {
            throw new InvalidCursorException("Failed to encode cursor", e);
        }
    }

    private ReportCursor decodeCursor(String cursorStr) {
        if (cursorStr == null || cursorStr.isBlank()) return null;
        try {
            byte[] bytes = java.util.Base64.getUrlDecoder().decode(cursorStr);
            return objectMapper.readValue(bytes, ReportCursor.class);
        } catch (Exception e) {
            throw new InvalidCursorException("Invalid cursor: " + e.getMessage(), e);
        }
    }
}
