package com.riksa.blog.service;

import com.riksa.auth.repository.UserRepository;
import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.event.BlogApprovedEvent;
import com.riksa.blog.event.BlogPostApprovedEvent;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.common.event.DomainEvent;
import com.riksa.common.event.EventPublisher;
import com.riksa.moderation.ContentBlockedException;
import com.riksa.moderation.ContentModerationService;
import com.riksa.moderation.AuthorSlidingWindowRateLimiter;
import com.riksa.moderation.ModerationProperties;
import com.riksa.moderation.RateLimitedException;
import com.riksa.moderation.WordListLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock private BlogPostRepository blogPostRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventPublisher eventPublisher;

    private ContentModerationService contentModerationService;
    private BlogModerationService blogModerationService;
    private BlogService blogService;

    @BeforeEach
    void setUp() {
        contentModerationService = spy(new ContentModerationService(
                new WordListLoader(),
                new ModerationProperties(5, 2, List.of("riksa.app"), Duration.ofHours(24))));
        blogModerationService = new BlogModerationService(blogPostRepository, eventPublisher);
        blogService = new BlogService(blogPostRepository, userRepository,
                contentModerationService, blogModerationService, new AuthorSlidingWindowRateLimiter());

        lenient().when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(invocation -> {
            BlogPost post = invocation.getArgument(0);
            if (post.getId() == null) {
                post.setId(UUID.randomUUID());
            }
            if (post.getCreatedAt() == null) {
                post.setCreatedAt(OffsetDateTime.now());
            }
            return post;
        });
    }

    @Test
    void createPost_allowPublishesNonStemQuestionAndFiresBothEventsOnce() {
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        BlogPost result = blogService.createPost(authorId, "How do I stay motivated before exams?",
                "I lose focus when exams are approaching. What helps?", null, categoryId);

        assertThat(result.getStatus()).isEqualTo(BlogStatus.APPROVED);
        assertThat(result.getAuthorId()).isEqualTo(authorId);
        assertThat(result.getCategoryId()).isEqualTo(categoryId);
        assertThat(result.getFirstApprovedAt()).isNotNull();
        assertThat(result.getModerationScore()).isZero();
        assertThat(result.getModerationReasons()).isEmpty();
        assertThat(result.getNormalizedHash()).hasSize(64);

        ArgumentCaptor<DomainEvent> events = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publish(events.capture());
        assertThat(events.getAllValues())
                .extracting(Object::getClass)
                .containsExactly(BlogApprovedEvent.class, BlogPostApprovedEvent.class);
        assertThat(((BlogApprovedEvent) events.getAllValues().get(0)).getModeratorId()).isNull();
    }

    @Test
    void createPost_reviewSavesPendingWithModerationDetailsAndNoEvents() {
        UUID authorId = UUID.randomUUID();
        when(blogPostRepository.existsByAuthorIdAndNormalizedHashAndCreatedAtAfter(
                eq(authorId), anyString(), any(OffsetDateTime.class))).thenReturn(true);

        BlogPost result = blogService.createPost(authorId, "A question", "A repeated question body",
                null, UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(BlogStatus.PENDING);
        assertThat(result.getModerationScore()).isEqualTo(3);
        assertThat(result.getModerationReasons()).isEqualTo("DUPLICATE");
        assertThat(result.getFirstApprovedAt()).isNull();
        verify(blogPostRepository).save(result);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void createPost_blockedContentSavesNothing() {
        UUID authorId = UUID.randomUUID();
        String linkHeavyBody = "https://a.example https://b.example https://c.example https://d.example";

        assertThatThrownBy(() -> blogService.createPost(authorId, "Links", linkHeavyBody,
                null, UUID.randomUUID()))
                .isInstanceOf(ContentBlockedException.class);

        verify(blogPostRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void updateApprovedPostDoesNotFireApprovalEventsAgain() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        OffsetDateTime firstApproval = OffsetDateTime.now().minusDays(1);
        BlogPost post = existingPost(postId, authorId, BlogStatus.APPROVED);
        post.setFirstApprovedAt(firstApproval);
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));

        BlogPost result = blogService.updatePost(authorId, postId, "Updated title", "Still a normal question.",
                null, UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(BlogStatus.APPROVED);
        assertThat(result.getFirstApprovedAt()).isEqualTo(firstApproval);
        assertThat(result.getTitle()).isEqualTo("Updated title");
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void updateIntoReviewReturnsPostToPending() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        BlogPost post = existingPost(postId, authorId, BlogStatus.APPROVED);
        post.setFirstApprovedAt(OffsetDateTime.now().minusDays(1));
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(blogPostRepository.existsByAuthorIdAndNormalizedHashAndCreatedAtAfterAndIdNot(
                eq(authorId), anyString(), any(OffsetDateTime.class), eq(postId))).thenReturn(true);

        BlogPost result = blogService.updatePost(authorId, postId, "Edited question", "Changed body",
                null, UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(BlogStatus.PENDING);
        assertThat(result.getModerationScore()).isEqualTo(3);
        assertThat(result.getModerationReasons()).isEqualTo("DUPLICATE");
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void rejectedPostEditedToAllowBecomesApproved() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        BlogPost post = existingPost(postId, authorId, BlogStatus.REJECTED);
        post.setRejectionReason("Please revise");
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));

        BlogPost result = blogService.updatePost(authorId, postId, "Revised question", "A clean new body.",
                null, UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(BlogStatus.APPROVED);
        assertThat(result.getRejectionReason()).isNull();
        assertThat(result.getFirstApprovedAt()).isNotNull();
        verify(eventPublisher, times(2)).publish(any());
    }

    @Test
    void authorEditCannotRestoreModeratorHiddenPost() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        BlogPost post = existingPost(postId, authorId, BlogStatus.HIDDEN);
        post.setHiddenReason("Reported content");
        post.setFirstApprovedAt(OffsetDateTime.now().minusDays(1));
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));

        BlogPost result = blogService.updatePost(authorId, postId, "Edited title", "A clean educational update.",
                null, UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(BlogStatus.HIDDEN);
        assertThat(result.getHiddenReason()).isEqualTo("Reported content");
        assertThat(result.getTitle()).isEqualTo("Edited title");
        verify(blogPostRepository).save(post);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void blockedEditLeavesPostUnchanged() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        BlogPost post = existingPost(postId, authorId, BlogStatus.APPROVED);
        post.setTitle("Original title");
        post.setContent("Original content");
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> blogService.updatePost(authorId, postId, "New title",
                "https://a.example https://b.example https://c.example https://d.example", null, null))
                .isInstanceOf(ContentBlockedException.class);

        assertThat(post.getTitle()).isEqualTo("Original title");
        assertThat(post.getContent()).isEqualTo("Original content");
        verify(blogPostRepository, never()).save(any());
    }

    @Test
    void fourthPostInTenMinutesIsRateLimitedBeforeModeration() {
        UUID authorId = UUID.randomUUID();
        when(blogPostRepository.countByAuthorIdAndCreatedAtAfter(eq(authorId), any(OffsetDateTime.class)))
                .thenReturn(3L);

        assertThatThrownBy(() -> blogService.createPost(authorId, "New", "A normal question.", null, null))
                .isInstanceOf(RateLimitedException.class);

        verify(contentModerationService, never()).assess(any(), any());
        verify(blogPostRepository, never()).save(any());
    }

    @Test
    void eleventhBlogEditInTenMinutesIsRateLimited() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        BlogPost post = existingPost(postId, authorId, BlogStatus.APPROVED);
        post.setFirstApprovedAt(OffsetDateTime.now().minusDays(1));
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));

        for (int edit = 0; edit < 10; edit++) {
            blogService.updatePost(authorId, postId, "Edited title " + edit,
                    "Clean educational update " + edit, null, null);
        }

        assertThatThrownBy(() -> blogService.updatePost(authorId, postId, "Too many edits",
                "Another clean educational update", null, null))
                .isInstanceOf(RateLimitedException.class);
        verify(blogPostRepository, times(10)).save(post);
    }

    @Test
    void nonOwnerCannotEditOrDelete() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        BlogPost post = existingPost(postId, ownerId, BlogStatus.APPROVED);
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> blogService.updatePost(otherUserId, postId, "title", "body", null, null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> blogService.deletePost(otherUserId, postId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        verify(blogPostRepository, never()).save(any());
        verify(blogPostRepository, never()).delete(any(BlogPost.class));
    }

    @Test
    void ownerCanDeletePost() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        BlogPost post = existingPost(postId, authorId, BlogStatus.APPROVED);
        when(blogPostRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));

        blogService.deletePost(authorId, postId);

        verify(blogPostRepository).delete(post);
    }

    @Test
    void getPost_existingPost_returnsPost() {
        UUID postId = UUID.randomUUID();
        BlogPost post = existingPost(postId, UUID.randomUUID(), BlogStatus.APPROVED);
        when(blogPostRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThat(blogService.getPost(postId)).isSameAs(post);
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
        when(blogPostRepository.findByStatus(eq(BlogStatus.APPROVED), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of()));

        assertThat(blogService.listPosts(BlogStatus.APPROVED, null, 0, 20).getContent()).isEmpty();
    }

    private BlogPost existingPost(UUID id, UUID authorId, BlogStatus status) {
        BlogPost post = new BlogPost();
        post.setId(id);
        post.setAuthorId(authorId);
        post.setStatus(status);
        post.setTitle("Existing title");
        post.setContent("Existing content");
        post.setCreatedAt(OffsetDateTime.now().minusDays(2));
        return post;
    }
}
