package com.riksa.blog.service;

import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.event.BlogApprovedEvent;
import com.riksa.blog.event.BlogPostApprovedEvent;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.common.audit.AuditLogService;
import com.riksa.common.event.DomainEvent;
import com.riksa.common.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogModerationServiceTest {

    @Mock private BlogPostRepository blogPostRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private BlogModerationService blogModerationService;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                blogModerationService, "auditLogService", auditLogService);
    }

    private final UUID moderatorId = UUID.randomUUID();

    @Test
    void approve_pendingPost_setsApproved() {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.PENDING);

        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost result = blogModerationService.approve(postId, moderatorId);

        assertThat(result.getStatus()).isEqualTo(BlogStatus.APPROVED);
        assertThat(result.getRejectionReason()).isNull();
        assertThat(result.getFirstApprovedAt()).isNotNull();
        org.mockito.ArgumentCaptor<DomainEvent> events = org.mockito.ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publish(events.capture());
        assertThat(events.getAllValues()).extracting(Object::getClass)
                .containsExactly(BlogApprovedEvent.class, BlogPostApprovedEvent.class);
    }

    @Test
    void approve_autoApprovedPostDoesNotDoubleFireEvents() {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.APPROVED);
        post.setFirstApprovedAt(OffsetDateTime.now().minusDays(1));

        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost result = blogModerationService.approve(postId, moderatorId);

        assertThat(result.getStatus()).isEqualTo(BlogStatus.APPROVED);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void approve_nonExistentPost_throwsException() {
        UUID postId = UUID.randomUUID();
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogModerationService.approve(postId, moderatorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Blog post not found");
    }

    @Test
    void reject_pendingPost_setsRejectedWithReason() {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.PENDING);

        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost result = blogModerationService.reject(postId, "Inappropriate content", moderatorId);

        assertThat(result.getStatus()).isEqualTo(BlogStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Inappropriate content");
    }

    @Test
    void reject_alreadyRejected_throwsException() {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.REJECTED);

        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> blogModerationService.reject(postId, "reason", moderatorId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Post already moderated");
    }

    @Test
    void reject_nonExistentPost_throwsException() {
        UUID postId = UUID.randomUUID();
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogModerationService.reject(postId, "reason", moderatorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Blog post not found");
    }

    @Test
    void hide_approvedPostRequiresReasonAndAudits() {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.APPROVED);
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost hidden = blogModerationService.hide(postId, "  Reported content  ", moderatorId);

        assertThat(hidden.getStatus()).isEqualTo(BlogStatus.HIDDEN);
        assertThat(hidden.getHiddenReason()).isEqualTo("Reported content");
        verify(auditLogService).record(moderatorId, "BLOG_HIDE", "BLOG", postId,
                Map.of("reason", "Reported content"));
    }

    @Test
    void approve_hiddenPostRestoresAndAudits() {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.HIDDEN);
        post.setHiddenReason("Reported content");
        post.setFirstApprovedAt(OffsetDateTime.now().minusDays(1));
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost restored = blogModerationService.approve(postId, moderatorId);

        assertThat(restored.getStatus()).isEqualTo(BlogStatus.APPROVED);
        assertThat(restored.getHiddenReason()).isNull();
        verify(auditLogService).record(moderatorId, "BLOG_RESTORE", "BLOG", postId,
                Map.of("previousReason", "Reported content"));
    }
}
