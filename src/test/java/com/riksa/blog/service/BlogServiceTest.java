package com.riksa.blog.service;

import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.blog.util.BadWordFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock private BlogPostRepository blogPostRepository;
    @Mock private BadWordFilter badWordFilter;
    @Mock private ContentFilterService contentFilterService;

    @InjectMocks private BlogService blogService;

    @Test
    void createPost_validContent_savesPost() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> {
            BlogPost p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        BlogPost result = blogService.createPost(authorId, "Physics in Daily Life",
                "Learning about physics is fun", "https://img.url/cover.png", categoryId);

        assertThat(result).isNotNull();
        assertThat(result.getAuthorId()).isEqualTo(authorId);
        assertThat(result.getStatus()).isEqualTo(BlogStatus.PENDING);
        assertThat(result.getCategoryId()).isEqualTo(categoryId);
        verify(badWordFilter).validate("Physics in Daily Life Learning about physics is fun");
        verify(contentFilterService).validate("Physics in Daily Life Learning about physics is fun");
    }

    @Test
    void createPost_offTopicContent_throwsException() {
        doThrow(new IllegalArgumentException("Content must be related to STEM, Tech, or History"))
                .when(contentFilterService).validate(anyString());

        assertThatThrownBy(() -> blogService.createPost(UUID.randomUUID(), "Hello",
                "world", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Content must be related to STEM, Tech, or History");

        verify(blogPostRepository, never()).save(any());
    }

    @Test
    void createPost_badWord_throwsException() {
        doThrow(new IllegalArgumentException("Content contains prohibited words"))
                .when(badWordFilter).validate(anyString());

        assertThatThrownBy(() -> blogService.createPost(UUID.randomUUID(), "spam",
                "scam content", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Content contains prohibited words");

        verify(blogPostRepository, never()).save(any());
    }

    @Test
    void getPost_existingPost_returnsPost() {
        UUID postId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(postId);
        post.setTitle("Test Post");

        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(post));

        BlogPost result = blogService.getPost(postId);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Post");
    }

    @Test
    void getPost_nonExistentPost_throwsException() {
        UUID postId = UUID.randomUUID();
        when(blogPostRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogService.getPost(postId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Blog post not found");
    }

    @Test
    void listPosts_byStatus_returnsPage() {
        BlogPost post1 = new BlogPost();
        post1.setId(UUID.randomUUID());
        post1.setStatus(BlogStatus.APPROVED);
        post1.setTitle("Approved Post");

        Page<BlogPost> page = new PageImpl<>(List.of(post1));
        when(blogPostRepository.findByStatus(eq(BlogStatus.APPROVED), any(PageRequest.class)))
                .thenReturn(page);

        Page<BlogPost> result = blogService.listPosts(BlogStatus.APPROVED, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Approved Post");
    }

    @Test
    void listPosts_byStatusAndCategory_returnsFilteredPage() {
        UUID categoryId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(UUID.randomUUID());
        post.setStatus(BlogStatus.APPROVED);

        Page<BlogPost> page = new PageImpl<>(List.of(post));
        when(blogPostRepository.findByStatusAndCategoryId(eq(BlogStatus.APPROVED), eq(categoryId),
                any(PageRequest.class)))
                .thenReturn(page);

        Page<BlogPost> result = blogService.listPosts(BlogStatus.APPROVED, categoryId, 0, 10);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getMyPosts_returnsAuthorPosts() {
        UUID authorId = UUID.randomUUID();
        BlogPost post = new BlogPost();
        post.setId(UUID.randomUUID());
        post.setAuthorId(authorId);

        Page<BlogPost> page = new PageImpl<>(List.of(post));
        when(blogPostRepository.findByAuthorId(eq(authorId), any(PageRequest.class)))
                .thenReturn(page);

        Page<BlogPost> result = blogService.getMyPosts(authorId, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAuthorId()).isEqualTo(authorId);
    }

    @Test
    void getMyPosts_withStatus_returnsFilteredAuthorPosts() {
        UUID authorId = UUID.randomUUID();
        Page<BlogPost> page = new PageImpl<>(List.of());
        when(blogPostRepository.findByAuthorIdAndStatus(
                eq(authorId), eq(BlogStatus.REJECTED), any(PageRequest.class)))
                .thenReturn(page);

        Page<BlogPost> result = blogService.getMyPosts(authorId, BlogStatus.REJECTED, 0, 10);

        assertThat(result).isEmpty();
        verify(blogPostRepository).findByAuthorIdAndStatus(
                eq(authorId), eq(BlogStatus.REJECTED), any(PageRequest.class));
    }
}
