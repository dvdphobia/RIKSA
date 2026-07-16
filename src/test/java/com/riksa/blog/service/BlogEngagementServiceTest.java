package com.riksa.blog.service;

import com.riksa.blog.entity.BlogLike;
import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.repository.BlogLikeRepository;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.blog.repository.BlogViewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogEngagementServiceTest {

    @Mock private BlogPostRepository blogPostRepository;
    @Mock private BlogViewRepository blogViewRepository;
    @Mock private BlogLikeRepository blogLikeRepository;

    @InjectMocks private BlogEngagementService blogEngagementService;

    @Test
    void trackView_incrementsViewCount() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setViewCount(5);

        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(post));
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
        post.setViewCount(0);

        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost result = blogEngagementService.trackView(postId, null);

        assertThat(result.getViewCount()).isEqualTo(1);
    }

    @Test
    void trackView_nonExistentPost_throwsException() {
        UUID postId = UUID.randomUUID();
        when(blogPostRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogEngagementService.trackView(postId, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Blog post not found");
    }

    @Test
    void toggleLike_newLike_incrementsLikeCount() {
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setLikeCount(0);

        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(post));
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
        post.setLikeCount(3);

        BlogLike existingLike = new BlogLike();
        existingLike.setUserId(userId);
        existingLike.setBlogPostId(postId);

        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(post));
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
        post.setLikeCount(0);

        BlogLike existingLike = new BlogLike();
        existingLike.setUserId(userId);
        existingLike.setBlogPostId(postId);

        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(post));
        when(blogLikeRepository.findByUserIdAndBlogPostId(userId, postId))
                .thenReturn(Optional.of(existingLike));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost result = blogEngagementService.toggleLike(postId, userId);

        assertThat(result.getLikeCount()).isEqualTo(0);
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