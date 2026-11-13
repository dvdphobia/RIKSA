package com.riksa.blog.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import com.riksa.blog.entity.BlogLike;
import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.repository.BlogLikeRepository;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.blog.repository.BlogViewRepository;
import com.riksa.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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
class BlogEngagementServiceTest {

    @Mock private BlogPostRepository blogPostRepository;
    @Mock private EntityManager entityManager;
    @Mock private BlogViewRepository blogViewRepository;
    @Mock private BlogLikeRepository blogLikeRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private BlogEngagementService blogEngagementService;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                blogEngagementService, "entityManager", entityManager);
    }

    @Test
    void trackView_incrementsViewCount() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.APPROVED);
        post.setViewCount(5);

        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost result = blogEngagementService.trackView(postId, userId);

        assertThat(result.getViewCount()).isEqualTo(6);
        verify(blogViewRepository).save(any());
    }

    @Test
    void trackView_guestUser_nullUserId_stillTracks() {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.APPROVED);
        post.setViewCount(0);

        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost result = blogEngagementService.trackView(postId, null);

        assertThat(result.getViewCount()).isEqualTo(1);
    }

    @Test
    void trackView_nonExistentPost_throwsException() {
        UUID postId = UUID.randomUUID();
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogEngagementService.trackView(postId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Blog post not found");

        verify(blogViewRepository, never()).save(any());
    }

    @Test
    void trackView_hiddenPostDoesNotRecordViewOrOverwriteModerationState() {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.APPROVED);
        post.setViewCount(5);
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        doAnswer(invocation -> {
            BlogPost refreshed = invocation.getArgument(0);
            refreshed.setStatus(BlogStatus.HIDDEN);
            refreshed.setHiddenReason("Reported content");
            return null;
        }).when(entityManager).refresh(post, LockModeType.PESSIMISTIC_WRITE);

        assertThatThrownBy(() -> blogEngagementService.trackView(postId, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Blog post not found");

        assertThat(post.getStatus()).isEqualTo(BlogStatus.HIDDEN);
        assertThat(post.getHiddenReason()).isEqualTo("Reported content");
        assertThat(post.getViewCount()).isEqualTo(5);
        verify(blogViewRepository, never()).save(any());
        verify(blogPostRepository, never()).save(any());
    }

    @Test
    void getPostForDetail_guestCannotReadHiddenPost() {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setAuthorId(UUID.randomUUID());
        post.setStatus(BlogStatus.HIDDEN);
        post.setHiddenReason("Reported content");
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> blogEngagementService.getPostForDetail(postId, null, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Blog post not found");

        verify(blogViewRepository, never()).save(any());
        verify(blogPostRepository, never()).save(any());
    }

    @Test
    void toggleLike_newLike_incrementsLikeCount() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.APPROVED);
        post.setAuthorId(UUID.randomUUID());
        post.setLikeCount(0);

        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(blogLikeRepository.findByUserIdAndBlogPostId(userId, postId))
                .thenReturn(Optional.empty());
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost result = blogEngagementService.toggleLike(postId, userId);

        assertThat(result.getLikeCount()).isEqualTo(1);
        verify(blogLikeRepository).save(any(BlogLike.class));
    }

    @Test
    void toggleLike_existingLike_decrementsLikeCount() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.APPROVED);
        post.setLikeCount(3);

        BlogLike existingLike = new BlogLike();
        existingLike.setUserId(userId);
        existingLike.setBlogPostId(postId);

        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(blogLikeRepository.findByUserIdAndBlogPostId(userId, postId))
                .thenReturn(Optional.of(existingLike));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost result = blogEngagementService.toggleLike(postId, userId);

        assertThat(result.getLikeCount()).isEqualTo(2);
        verify(blogLikeRepository).delete(existingLike);
        verify(blogLikeRepository, never()).save(any());
    }

    @Test
    void toggleLike_likeCountDoesNotGoNegative() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.APPROVED);
        post.setLikeCount(0);

        BlogLike existingLike = new BlogLike();
        existingLike.setUserId(userId);
        existingLike.setBlogPostId(postId);

        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(blogLikeRepository.findByUserIdAndBlogPostId(userId, postId))
                .thenReturn(Optional.of(existingLike));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost result = blogEngagementService.toggleLike(postId, userId);

        assertThat(result.getLikeCount()).isEqualTo(0);
    }

    @Test
    void toggleLike_hiddenPostDoesNotReturnContentOrChangeModerationState() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setStatus(BlogStatus.HIDDEN);
        post.setHiddenReason("Reported content");
        post.setLikeCount(3);
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> blogEngagementService.toggleLike(postId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Blog post not found");

        assertThat(post.getStatus()).isEqualTo(BlogStatus.HIDDEN);
        assertThat(post.getHiddenReason()).isEqualTo("Reported content");
        assertThat(post.getLikeCount()).isEqualTo(3);
        verify(blogLikeRepository, never()).findByUserIdAndBlogPostId(any(), any());
        verify(blogLikeRepository, never()).save(any());
        verify(blogLikeRepository, never()).delete(any());
        verify(blogPostRepository, never()).save(any());
    }

    @Test
    void isLiked_liked_returnsTrue() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(blogLikeRepository.existsByUserIdAndBlogPostId(userId, postId)).thenReturn(true);

        boolean result = blogEngagementService.isLiked(postId, userId);

        assertThat(result).isTrue();
    }

    @Test
    void isLiked_notLiked_returnsFalse() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(blogLikeRepository.existsByUserIdAndBlogPostId(userId, postId)).thenReturn(false);

        boolean result = blogEngagementService.isLiked(postId, userId);

        assertThat(result).isFalse();
    }
}
