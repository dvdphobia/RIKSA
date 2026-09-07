package com.riksa.social.service;

import com.riksa.common.event.EventPublisher;
import com.riksa.social.entity.Post;
import com.riksa.social.event.PostCreatedEvent;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.UserCategoryMembershipRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private UserCategoryMembershipRepository membershipRepository;
    @Mock private MediaUrlValidator mediaUrlValidator;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks private PostService postService;

    @Test
    void createPost_validContent_savesAndPublishesEvent() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        when(membershipRepository.existsByUserIdAndCategoryId(authorId, categoryId))
                .thenReturn(true);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        Post result = postService.createPost(authorId, "Physics Question",
                "Help with physics homework", null, categoryId);

        assertThat(result).isNotNull();
        assertThat(result.getAuthorId()).isEqualTo(authorId);
        assertThat(result.getCategoryId()).isEqualTo(categoryId);
        verify(eventPublisher).publish(any(PostCreatedEvent.class));
    }

    @Test
    void createPost_notMemberOfCategory_throwsException() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        when(membershipRepository.existsByUserIdAndCategoryId(authorId, categoryId))
                .thenReturn(false);

        assertThatThrownBy(() -> postService.createPost(authorId, "Test", "physics", null, categoryId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You must join this category before posting");

        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_arbitraryContent_isAllowed() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        when(membershipRepository.existsByUserIdAndCategoryId(authorId, categoryId))
                .thenReturn(true);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        // The community feed carries no topic restriction: only the blog does.
        Post result = postService.createPost(authorId, "Hello", "world", null, categoryId);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Hello");
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void createPost_storesTheNormalizedMediaUrl() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        when(membershipRepository.existsByUserIdAndCategoryId(authorId, categoryId))
                .thenReturn(true);
        when(mediaUrlValidator.normalize("  https://cdn.example.com/a.png  "))
                .thenReturn("https://cdn.example.com/a.png");
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post result = postService.createPost(authorId, "Geometry", "See the diagram",
                "  https://cdn.example.com/a.png  ", categoryId);

        // The raw client value must never reach the column unchecked.
        assertThat(result.getMediaUrl()).isEqualTo("https://cdn.example.com/a.png");
    }

    @Test
    void createPost_rejectsAnUnusableMediaUrl() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        when(membershipRepository.existsByUserIdAndCategoryId(authorId, categoryId))
                .thenReturn(true);
        doThrow(new IllegalArgumentException("Media URL must use http or https"))
                .when(mediaUrlValidator).normalize(anyString());

        assertThatThrownBy(() -> postService.createPost(authorId, "T", "C",
                "javascript:alert(1)", categoryId))
                .isInstanceOf(IllegalArgumentException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    void updatePost_nullMediaLeavesTheImageAlone() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(authorId);
        post.setMediaUrl("https://cdn.example.com/keep.png");

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post result = postService.updatePost(authorId, postId, "New title", null, null);

        assertThat(result.getMediaUrl()).isEqualTo("https://cdn.example.com/keep.png");
        verify(mediaUrlValidator, never()).normalize(any());
    }

    @Test
    void updatePost_blankMediaClearsTheImage() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(authorId);
        post.setMediaUrl("https://cdn.example.com/old.png");

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(mediaUrlValidator.normalize("")).thenReturn(null);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post result = postService.updatePost(authorId, postId, null, null, "");

        assertThat(result.getMediaUrl()).isNull();
    }

    @Test
    void updatePost_byAuthor_updatesFields() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(authorId);
        post.setTitle("Old Title");
        post.setContent("Old content");

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post result = postService.updatePost(authorId, postId, "New Title", "New physics content", null);

        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getContent()).isEqualTo("New physics content");
    }

    @Test
    void updatePost_byNonAuthor_throwsException() {
        UUID authorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(otherUserId);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(authorId, postId, "New", "content", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only edit your own posts");
    }

    @Test
    void updatePost_nonExistentPost_throwsException() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost(UUID.randomUUID(), postId, "New", "content", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");
    }

    @Test
    void deletePost_byAuthor_deletesPost() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(authorId);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        postService.deletePost(authorId, postId);

        verify(postRepository).delete(post);
    }

    @Test
    void deletePost_byNonAuthor_throwsException() {
        UUID authorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(otherUserId);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(authorId, postId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only delete your own posts");

        verify(postRepository, never()).delete(any());
    }

    @Test
    void getPost_existingPost_returnsPost() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setTitle("Test");

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Post result = postService.getPost(postId);

        assertThat(result.getTitle()).isEqualTo("Test");
    }

    @Test
    void getPost_nonExistentPost_throwsException() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(postId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");
    }

    @Test
    void getVisiblePost_visiblePost_returnsPost() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThat(postService.getVisiblePost(postId)).isSameAs(post);
    }

    @Test
    void getVisiblePost_hiddenPost_throwsException() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setStatus(com.riksa.social.enums.ContentStatus.HIDDEN);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.getVisiblePost(postId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");
    }
}
