package com.riksa.report.service;

import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.report.dto.CreateReportRequest;
import com.riksa.report.dto.ReportResponse;
import com.riksa.report.entity.ContentReport;
import com.riksa.report.enums.ReportStatus;
import com.riksa.report.repository.ContentReportRepository;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ContentReportRepository reportRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private BlogPostRepository blogPostRepository;

    @InjectMocks private ReportService reportService;

    private final UUID reporterId = UUID.randomUUID();

    @Test
    void fileReport_validPostTarget_savesAndMapsResponse() {
        UUID targetId = UUID.randomUUID();
        CreateReportRequest request = request("POST", targetId, "SPAM", "same post 10 times");

        when(postRepository.existsById(targetId)).thenReturn(true);
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId,
                com.riksa.report.enums.ReportTargetType.POST, targetId)).thenReturn(false);
        when(reportRepository.save(any(ContentReport.class))).thenAnswer(inv -> {
            ContentReport r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ReportResponse response = reportService.fileReport(reporterId, request);

        ArgumentCaptor<ContentReport> captor = ArgumentCaptor.forClass(ContentReport.class);
        verify(reportRepository).save(captor.capture());
        ContentReport saved = captor.getValue();
        assertThat(saved.getReporterId()).isEqualTo(reporterId);
        assertThat(saved.getTargetId()).isEqualTo(targetId);
        assertThat(saved.getReason().name()).isEqualTo("SPAM");
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.OPEN);

        assertThat(response.getStatus()).isEqualTo("OPEN");
        assertThat(response.getTargetType()).isEqualTo("POST");
        assertThat(response.getDetails()).isEqualTo("same post 10 times");
    }

    @Test
    void fileReport_commentTarget_checksCommentRepository() {
        UUID targetId = UUID.randomUUID();
        CreateReportRequest request = request("COMMENT", targetId, "HARASSMENT", null);

        when(commentRepository.existsById(targetId)).thenReturn(true);
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId,
                com.riksa.report.enums.ReportTargetType.COMMENT, targetId)).thenReturn(false);
        when(reportRepository.save(any(ContentReport.class))).thenAnswer(inv -> inv.getArgument(0));

        reportService.fileReport(reporterId, request);

        verify(commentRepository).existsById(targetId);
        verifyNoInteractions(postRepository, blogPostRepository);
    }

    @Test
    void fileReport_duplicate_throwsConflict() {
        UUID targetId = UUID.randomUUID();
        CreateReportRequest request = request("POST", targetId, "SPAM", null);

        when(postRepository.existsById(targetId)).thenReturn(true);
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId,
                com.riksa.report.enums.ReportTargetType.POST, targetId)).thenReturn(true);

        assertThatThrownBy(() -> reportService.fileReport(reporterId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You have already reported this content");

        verify(reportRepository, never()).save(any());
    }

    @Test
    void fileReport_nonExistentPost_throwsNotFound() {
        UUID targetId = UUID.randomUUID();
        when(postRepository.existsById(targetId)).thenReturn(false);

        assertThatThrownBy(() -> reportService.fileReport(reporterId, request("POST", targetId, "SPAM", null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("post not found");

        verify(reportRepository, never()).save(any());
    }

    @Test
    void fileReport_nonExistentBlog_throwsNotFound() {
        UUID targetId = UUID.randomUUID();
        when(blogPostRepository.existsById(targetId)).thenReturn(false);

        assertThatThrownBy(() -> reportService.fileReport(reporterId, request("BLOG", targetId, "SPAM", null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("blog not found");
    }

    @Test
    void fileReport_invalidTargetType_throwsBadRequest() {
        assertThatThrownBy(() -> reportService.fileReport(reporterId,
                request("VIDEO", UUID.randomUUID(), "SPAM", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid targetType");
    }

    @Test
    void fileReport_invalidReason_throwsBadRequest() {
        assertThatThrownBy(() -> reportService.fileReport(reporterId,
                request("POST", UUID.randomUUID(), "MEAN", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid reason");
    }

    private CreateReportRequest request(String targetType, UUID targetId, String reason, String details) {
        CreateReportRequest request = new CreateReportRequest();
        request.setTargetType(targetType);
        request.setTargetId(targetId);
        request.setReason(reason);
        request.setDetails(details);
        return request;
    }
}
