package com.riksa.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.riksa.admin.dto.ReportResolutionRequest;
import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.blog.service.BlogModerationService;
import com.riksa.common.audit.AuditLogService;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.gamification.dto.CursorPage;
import com.riksa.report.dto.ReportResponse;
import com.riksa.report.entity.ContentReport;
import com.riksa.report.enums.ReportAction;
import com.riksa.report.enums.ReportReason;
import com.riksa.report.enums.ReportStatus;
import com.riksa.report.enums.ReportTargetType;
import com.riksa.report.repository.ContentReportRepository;
import com.riksa.resource.repository.LessonResourceRepository;
import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.enums.ContentStatus;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportAdminServiceTest {

    @Mock private ContentReportRepository reportRepository;
    @Mock private BlogPostRepository blogPostRepository;
    @Mock private BlogModerationService blogModerationService;
    @Mock private PostRepository postRepository;
    @Mock private LessonResourceRepository lessonResourceRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private SocialModerationService socialModerationService;
    @Mock private AuditLogService auditLogService;

    private ReportAdminService service;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final UUID moderatorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ReportAdminService(reportRepository, blogPostRepository, blogModerationService,
                postRepository, lessonResourceRepository, commentRepository, socialModerationService,
                auditLogService, objectMapper);
    }

    @Test
    void list_firstPage_defaultsToOpenAndReturnsCursor() {
        ContentReport first = report(ReportTargetType.POST, OffsetDateTime.now().minusHours(2));
        ContentReport second = report(ReportTargetType.POST, OffsetDateTime.now().minusHours(1));
        when(reportRepository.findFirstPage(eq(ReportStatus.OPEN), eq(null), any(PageRequest.class)))
                .thenReturn(List.of(first, second));

        CursorPage<ReportResponse> page = service.list(null, null, null, 1);

        assertThat(page.getEdges()).hasSize(1);
        assertThat(page.isHasNextPage()).isTrue();
        assertThat(page.getEndCursor()).isNotNull();
        verify(reportRepository).findFirstPage(ReportStatus.OPEN, null, PageRequest.of(0, 2));
    }

    @Test
    void list_withCursor_decodesAndPassesThrough() throws Exception {
        OffsetDateTime cursorTime = OffsetDateTime.now().minusHours(1);
        UUID cursorId = UUID.randomUUID();
        String cursor = Base64.getUrlEncoder().withoutPadding().encodeToString(
                objectMapper.writeValueAsBytes(Map.of("t", cursorTime, "i", cursorId)));

        when(reportRepository.findAfterCursor(any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(List.of());

        service.list("RESOLVED", "COMMENT", cursor, 10);

        // Jackson normalizes the offset to UTC on decode — same instant, so queries behave identically
        verify(reportRepository).findAfterCursor(eq(ReportStatus.RESOLVED), eq(ReportTargetType.COMMENT),
                argThat(t -> t != null && t.toInstant().equals(cursorTime.toInstant())),
                eq(cursorId), eq(PageRequest.of(0, 11)));
    }

    @Test
    void list_invalidStatus_throwsBadRequest() {
        assertThatThrownBy(() -> service.list("BANISHED", null, null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status");
    }

    @Test
    void resolve_resolvedNoAction_setsFieldsAndAudits() {
        ContentReport reportEntity = report(ReportTargetType.POST, OffsetDateTime.now().minusHours(1));
        UUID reportId = UUID.randomUUID();
        reportEntity.setId(reportId);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));
        when(reportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));

        ReportResponse response = service.resolve(moderatorId, reportId, resolution(ReportStatus.RESOLVED, null));

        assertThat(reportEntity.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(reportEntity.getResolvedBy()).isEqualTo(moderatorId);
        assertThat(reportEntity.getResolvedAt()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("RESOLVED");

        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).record(eq(moderatorId), eq("REPORT_RESOLVED"), eq("REPORT"), eq(reportId), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue())
                .containsEntry("action", "NONE")
                .containsEntry("targetType", "POST");
        verifyNoInteractions(socialModerationService);
    }

    @Test
    void resolve_dismissed_setsFieldsAndAudits() {
        ContentReport reportEntity = report(ReportTargetType.COMMENT, OffsetDateTime.now().minusHours(1));
        UUID reportId = UUID.randomUUID();
        reportEntity.setId(reportId);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));
        when(reportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));

        service.resolve(moderatorId, reportId, resolution(ReportStatus.DISMISSED, ReportAction.NONE));

        assertThat(reportEntity.getStatus()).isEqualTo(ReportStatus.DISMISSED);
        verify(auditLogService).record(eq(moderatorId), eq("REPORT_DISMISSED"), eq("REPORT"), eq(reportId), any());
        verifyNoInteractions(socialModerationService);
    }

    @Test
    void resolve_resolvedWithHidePost_hidesTargetAndAudits() {
        UUID targetId = UUID.randomUUID();
        ContentReport reportEntity = report(ReportTargetType.POST, OffsetDateTime.now().minusHours(1));
        reportEntity.setTargetId(targetId);
        reportEntity.setDetails("buy now");
        UUID reportId = UUID.randomUUID();
        reportEntity.setId(reportId);

        Post post = new Post();
        post.setId(targetId);

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));
        when(reportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postRepository.findById(targetId)).thenReturn(Optional.of(post));

        service.resolve(moderatorId, reportId, resolution(ReportStatus.RESOLVED, ReportAction.HIDE));

        verify(socialModerationService).setPostStatus(moderatorId, targetId,
                ContentStatus.HIDDEN, "Reported as SPAM: buy now");
        assertThat(reportEntity.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    void resolve_resolvedWithHideComment_hidesTarget() {
        UUID targetId = UUID.randomUUID();
        ContentReport reportEntity = report(ReportTargetType.COMMENT, OffsetDateTime.now().minusHours(1));
        reportEntity.setTargetId(targetId);
        UUID reportId = UUID.randomUUID();
        reportEntity.setId(reportId);

        Comment comment = new Comment();
        comment.setId(targetId);

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));
        when(reportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commentRepository.findById(targetId)).thenReturn(Optional.of(comment));

        service.resolve(moderatorId, reportId, resolution(ReportStatus.RESOLVED, ReportAction.HIDE));

        verify(socialModerationService).setCommentStatus(eq(moderatorId), eq(targetId),
                eq(ContentStatus.HIDDEN), any());
    }

    @Test
    void resolve_hideAlreadyHiddenTarget_skipsHideButResolves() {
        UUID targetId = UUID.randomUUID();
        ContentReport reportEntity = report(ReportTargetType.POST, OffsetDateTime.now().minusHours(1));
        reportEntity.setTargetId(targetId);
        UUID reportId = UUID.randomUUID();
        reportEntity.setId(reportId);

        Post post = new Post();
        post.setId(targetId);
        post.setStatus(ContentStatus.HIDDEN);

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));
        when(reportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postRepository.findById(targetId)).thenReturn(Optional.of(post));

        service.resolve(moderatorId, reportId, resolution(ReportStatus.RESOLVED, ReportAction.HIDE));

        verifyNoInteractions(socialModerationService);
        assertThat(reportEntity.getStatus()).isEqualTo(ReportStatus.RESOLVED);

        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).record(eq(moderatorId), eq("REPORT_RESOLVED"), eq("REPORT"), eq(reportId), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue()).containsEntry("hide", "alreadyHidden");
    }

    @Test
    void resolve_hideMissingTarget_skipsHideButResolves() {
        UUID targetId = UUID.randomUUID();
        ContentReport reportEntity = report(ReportTargetType.POST, OffsetDateTime.now().minusHours(1));
        reportEntity.setTargetId(targetId);
        UUID reportId = UUID.randomUUID();
        reportEntity.setId(reportId);

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));
        when(reportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postRepository.findById(targetId)).thenReturn(Optional.empty());

        service.resolve(moderatorId, reportId, resolution(ReportStatus.RESOLVED, ReportAction.HIDE));

        verifyNoInteractions(socialModerationService);
        assertThat(reportEntity.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    void resolve_resolvedWithHideBlog_hidesPublishedTarget() {
        ContentReport reportEntity = report(ReportTargetType.BLOG, OffsetDateTime.now().minusHours(1));
        UUID targetId = reportEntity.getTargetId();
        BlogPost blogPost = new BlogPost();
        blogPost.setId(targetId);
        blogPost.setStatus(BlogStatus.APPROVED);
        UUID reportId = UUID.randomUUID();
        reportEntity.setId(reportId);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));
        when(reportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(blogPostRepository.findById(targetId)).thenReturn(Optional.of(blogPost));

        service.resolve(moderatorId, reportId, resolution(ReportStatus.RESOLVED, ReportAction.HIDE));

        assertThat(reportEntity.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        verify(blogModerationService).hide(targetId, "Reported as SPAM", moderatorId);
    }

    @Test
    void resolve_hideAlreadyHiddenBlog_skipsHideButResolves() {
        ContentReport reportEntity = report(ReportTargetType.BLOG, OffsetDateTime.now().minusHours(1));
        UUID targetId = reportEntity.getTargetId();
        BlogPost blogPost = new BlogPost();
        blogPost.setId(targetId);
        blogPost.setStatus(BlogStatus.HIDDEN);
        UUID reportId = UUID.randomUUID();
        reportEntity.setId(reportId);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));
        when(reportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(blogPostRepository.findById(targetId)).thenReturn(Optional.of(blogPost));

        service.resolve(moderatorId, reportId, resolution(ReportStatus.RESOLVED, ReportAction.HIDE));

        verifyNoInteractions(blogModerationService);
        assertThat(reportEntity.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    void resolve_alreadyResolved_throwsBadRequest() {
        ContentReport reportEntity = report(ReportTargetType.POST, OffsetDateTime.now().minusHours(1));
        reportEntity.setStatus(ReportStatus.RESOLVED);
        UUID reportId = UUID.randomUUID();
        reportEntity.setId(reportId);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));

        assertThatThrownBy(() -> service.resolve(moderatorId, reportId, resolution(ReportStatus.DISMISSED, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Report is already RESOLVED");
    }

    @Test
    void resolve_missingReport_throwsNotFound() {
        UUID reportId = UUID.randomUUID();
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(moderatorId, reportId, resolution(ReportStatus.RESOLVED, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report not found");
    }

    @Test
    void get_existingReport_returnsResponse() {
        ContentReport reportEntity = report(ReportTargetType.POST, OffsetDateTime.now().minusHours(1));
        UUID reportId = UUID.randomUUID();
        reportEntity.setId(reportId);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));

        ReportResponse response = service.get(reportId);

        assertThat(response.getId()).isEqualTo(reportId);
        assertThat(response.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void get_missingReport_throwsNotFound() {
        UUID reportId = UUID.randomUUID();
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(reportId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report not found");
    }

    @Test
    void resolve_openStatusRequest_throwsBadRequest() {
        ContentReport reportEntity = report(ReportTargetType.POST, OffsetDateTime.now().minusHours(1));
        UUID reportId = UUID.randomUUID();
        reportEntity.setId(reportId);
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(reportEntity));

        assertThatThrownBy(() -> service.resolve(moderatorId, reportId, resolution(ReportStatus.OPEN, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot set status to OPEN");
    }

    private ContentReport report(ReportTargetType targetType, OffsetDateTime createdAt) {
        ContentReport report = new ContentReport();
        report.setReporterId(UUID.randomUUID());
        report.setTargetType(targetType);
        report.setTargetId(UUID.randomUUID());
        report.setReason(ReportReason.SPAM);
        report.setStatus(ReportStatus.OPEN);
        report.setCreatedAt(createdAt);
        return report;
    }

    private ReportResolutionRequest resolution(ReportStatus status, ReportAction action) {
        ReportResolutionRequest request = new ReportResolutionRequest();
        request.setStatus(status);
        request.setAction(action);
        return request;
    }
}
