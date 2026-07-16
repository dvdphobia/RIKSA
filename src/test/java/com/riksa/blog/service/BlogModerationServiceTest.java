package com.riksa.blog.service;

import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.repository.BlogPostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogModerationServiceTest {

    @Mock private BlogPostRepository blogPostRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private BlogModerationService blogModerationService;

    @Test
    void approve_pendingPost_setsApproved() {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.PENDING);

        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost result = blogModerationService.approve(postId);

        assertThat(result.getStatus()).isEqualTo(BlogStatus.APPROVED);
        assertThat(result.getRejectionReason()).isNull();
    }

    @Test
    void approve_alreadyApproved_throwsException() {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.APPROVED);

        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> blogModerationService.approve(postId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Post already moderated");
    }

    @Test
    void approve_nonExistentPost_throwsException() {
        UUID postId = UUID.randomUUID();
        when(blogPostRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogModerationService.approve(postId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Blog post not found");
    }

    @Test
    void reject_pendingPost_setsRejectedWithReason() {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.PENDING);

        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost result = blogModerationService.reject(postId, "Inappropriate content");

        assertThat(result.getStatus()).isEqualTo(BlogStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Inappropriate content");
    }

    @Test
    void reject_alreadyRejected_throwsException() {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.REJECTED);

        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> blogModerationService.reject(postId, "reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Post already moderated");
    }

    @Test
    void reject_nonExistentPost_throwsException() {
        UUID postId = UUID.randomUUID();
        when(blogPostRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogModerationService.reject(postId, "reason"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Blog post not found");
    }
}