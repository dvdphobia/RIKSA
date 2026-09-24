package com.riksa.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.report.dto.CreateReportRequest;
import com.riksa.report.entity.ContentReport;
import com.riksa.report.repository.ContentReportRepository;
import com.riksa.report.service.ReportService;
import com.riksa.resource.repository.LessonResourceRepository;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResourceReportTest {
    @Test
    void resourceCanBeReportedThroughTheExistingReportFlow() {
        var reports = mock(ContentReportRepository.class);
        var posts = mock(PostRepository.class);
        var comments = mock(CommentRepository.class);
        var blogs = mock(BlogPostRepository.class);
        var resources = mock(LessonResourceRepository.class);
        var service = new ReportService(reports, posts, comments, blogs, resources);
        UUID reporterId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        CreateReportRequest request = new CreateReportRequest();
        request.setTargetType("RESOURCE");
        request.setTargetId(resourceId);
        request.setReason("OTHER");
        when(resources.existsById(resourceId)).thenReturn(true);
        when(reports.existsByReporterIdAndTargetTypeAndTargetId(eq(reporterId), any(), eq(resourceId)))
                .thenReturn(false);
        when(reports.save(any(ContentReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.fileReport(reporterId, request);

        assertEquals("RESOURCE", result.getTargetType());
        assertEquals(resourceId, result.getTargetId());
    }
}
