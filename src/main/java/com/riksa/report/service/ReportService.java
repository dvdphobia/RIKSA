package com.riksa.report.service;

import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.report.dto.CreateReportRequest;
import com.riksa.report.dto.ReportResponse;
import com.riksa.report.entity.ContentReport;
import com.riksa.report.enums.ReportReason;
import com.riksa.report.enums.ReportTargetType;
import com.riksa.report.repository.ContentReportRepository;
import com.riksa.resource.repository.LessonResourceRepository;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ContentReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final BlogPostRepository blogPostRepository;
    private final LessonResourceRepository lessonResourceRepository;

    @Transactional
    public ReportResponse fileReport(UUID reporterId, CreateReportRequest request) {
        ReportTargetType targetType = parseTargetType(request.getTargetType());
        ReportReason reason = parseReason(request.getReason());

        verifyTargetExists(targetType, request.getTargetId());

        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, targetType, request.getTargetId())) {
            throw new IllegalStateException("You have already reported this content");
        }

        ContentReport report = new ContentReport();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(request.getTargetId());
        report.setReason(reason);
        report.setDetails(request.getDetails());

        return toResponse(reportRepository.save(report));
    }

    private void verifyTargetExists(ReportTargetType targetType, UUID targetId) {
        boolean exists = switch (targetType) {
            case POST -> postRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
            case BLOG -> blogPostRepository.existsById(targetId);
            case RESOURCE -> lessonResourceRepository.existsById(targetId);
        };
        if (!exists) {
            throw new ResourceNotFoundException(
                    targetType.name().toLowerCase() + " not found: " + targetId);
        }
    }

    public static ReportTargetType parseTargetType(String value) {
        try {
            return ReportTargetType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Invalid targetType: " + value + ". Valid values: POST, COMMENT, BLOG, RESOURCE");
        }
    }

    public static ReportReason parseReason(String value) {
        try {
            return ReportReason.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Invalid reason: " + value + ". Valid values: SPAM, HARASSMENT, NSFW, MISINFORMATION, OTHER");
        }
    }

    public static ReportResponse toResponse(ContentReport report) {
        return new ReportResponse(
                report.getId(),
                report.getReporterId(),
                report.getTargetType().name(),
                report.getTargetId(),
                report.getReason().name(),
                report.getDetails(),
                report.getStatus().name(),
                report.getResolvedBy(),
                report.getResolvedAt(),
                report.getCreatedAt()
        );
    }
}
