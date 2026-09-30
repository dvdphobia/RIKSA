package com.riksa.social.service;

import com.riksa.notification.service.NotificationService;
import com.riksa.notification.entity.NotificationType;
import com.riksa.auth.repository.UserRepository;
import com.riksa.moderation.ContentBlockedException;
import com.riksa.moderation.ContentModerationService;
import com.riksa.moderation.AuthorSlidingWindowRateLimiter;
import com.riksa.moderation.ModerationProperties;
import com.riksa.moderation.RateLimitedException;
import com.riksa.moderation.WordListLoader;
import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private MediaUrlValidator mediaUrlValidator;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        ContentModerationService moderation = new ContentModerationService(
                new WordListLoader(), new ModerationProperties(5, 2, List.of(), Duration.ofHours(24)));
        commentService = new CommentService(commentRepository, postRepository, mediaUrlValidator,
                userRepository, moderation, new AuthorSlidingWindowRateLimiter());
        ReflectionTestUtils.setField(commentService, "eventPublisher", eventPublisher);
    }

    @Test
    void createComment_validContent_savesAndIncrementsCount() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setCommentCount(5);

        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Comment result = commentService.createComment(authorId, postId, "Great physics explanation", null, null);

        assertThat(result).isNotNull();
        assertThat(result.getAuthorId()).isEqualTo(authorId);
        assertThat(result.getPostId()).isEqualTo(postId);
        assertThat(post.getCommentCount()).isEqualTo(6);
    }

    @Test
    void createComment_severeTermIsBlockedBeforeCommentOrPostSave() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.createComment(authorId, postId, "shit", null, null))
                .isInstanceOf(ContentBlockedException.class);

        verify(commentRepository, never()).save(any());
        verify(postRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void createComment_eleventhInTenMinutesIsRateLimitedBeforeModerationOrSave() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.countByAuthorIdAndCreatedAtAfter(
                eq(authorId), any(OffsetDateTime.class))).thenReturn(10L);

        assertThatThrownBy(() -> commentService.createComment(authorId, postId, "A normal reply", null, null))
                .isInstanceOf(RateLimitedException.class);

        verify(commentRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
        verify(postRepository, never()).save(any());
    }

    @Test
    void createComment_hiddenPost_throwsException() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setStatus(com.riksa.social.enums.ContentStatus.HIDDEN);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.createComment(authorId, postId, "physics content", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void createComment_nonExistentPost_throwsException() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(UUID.randomUUID(), postId, "physics content", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");
    }

    @Test
    void updateComment_byAuthor_updatesContent() {
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthorId(authorId);
        comment.setContent("Old content");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        Comment result = commentService.updateComment(authorId, commentId, "Updated physics content", null);

        assertThat(result.getContent()).isEqualTo("Updated physics content");
    }

    @Test
    void eleventhEditOfAnOldCommentInTenMinutesIsRateLimited() {
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthorId(authorId);
        comment.setContent("Old educational comment");
        comment.setCreatedAt(OffsetDateTime.now().minusDays(2));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.findByAuthorIdAndCreatedAtAfterOrderByCreatedAtDescIdDesc(any(), any(), any()))
                .thenReturn(List.of());
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        for (int edit = 0; edit < 10; edit++) {
            commentService.updateComment(authorId, commentId, "Updated study comment " + edit, null);
        }

        assertThatThrownBy(() -> commentService.updateComment(authorId, commentId,
                "Too many study comment edits", null)).isInstanceOf(RateLimitedException.class);
    }

    @Test
    void updateComment_severeTermIsBlockedWithoutChangingOrSavingComment() {
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthorId(authorId);
        comment.setContent("Old content");
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(authorId, commentId, "shit", null))
                .isInstanceOf(ContentBlockedException.class);

        assertThat(comment.getContent()).isEqualTo("Old content");
        verify(commentRepository, never()).save(any());
        verify(postRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void updateComment_byNonAuthor_throwsException() {
        UUID authorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthorId(otherUserId);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(authorId, commentId, "new content", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only edit your own comments");
    }

    @Test
    void deleteComment_byAuthor_deletesAndDecrementsCount() {
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthorId(authorId);
        comment.setPostId(postId);
        Post post = new Post();
        post.setId(postId);
        post.setCommentCount(3);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        commentService.deleteComment(authorId, commentId);

        verify(commentRepository).delete(comment);
        assertThat(post.getCommentCount()).isEqualTo(2);
    }

    @Test
    void deleteComment_byNonAuthor_throwsException() {
        UUID authorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthorId(otherUserId);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(authorId, commentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You can only delete your own comments");
    }

    @Test
    void deleteComment_commentCountDoesNotGoNegative() {
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthorId(authorId);
        comment.setPostId(postId);
        Post post = new Post();
        post.setId(postId);
        post.setCommentCount(0);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        commentService.deleteComment(authorId, commentId);

        assertThat(post.getCommentCount()).isEqualTo(0);
    }

    @Test
    void getComment_nonExistent_throwsException() {
        UUID commentId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getComment(commentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Comment not found");
    }

    // ── Image comments ───────────────────────────────────

    @Test
    void createComment_withTextAndImage_savesBoth() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        String url = "https://cdn.example.com/diagram.png";

        when(mediaUrlValidator.normalize(url)).thenReturn(url);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Comment result = commentService.createComment(authorId, postId, "physics diagram", url, null);

        assertThat(result.getContent()).isEqualTo("physics diagram");
        assertThat(result.getMediaUrl()).isEqualTo(url);
    }

    @Test
    void createComment_imageOnly_skipsStemFilter() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        String url = "https://cdn.example.com/photo.jpg";

        when(mediaUrlValidator.normalize(url)).thenReturn(url);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Comment result = commentService.createComment(authorId, postId, null, url, null);

        assertThat(result.getContent()).isNull();
        assertThat(result.getMediaUrl()).isEqualTo(url);
    }

    @Test
    void createComment_blankContentIsTreatedAsAbsent() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        String url = "https://cdn.example.com/photo.jpg";

        when(mediaUrlValidator.normalize(url)).thenReturn(url);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Comment result = commentService.createComment(UUID.randomUUID(), postId, "   ", url, null);

        assertThat(result.getContent()).isNull();
    }

    @Test
    void createComment_neitherTextNorImage_throwsException() {
        assertThatThrownBy(() -> commentService.createComment(UUID.randomUUID(), UUID.randomUUID(), "  ", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A comment must have text or an image");

        verify(commentRepository, never()).save(any());
        verifyNoInteractions(postRepository);
    }

    @Test
    void createComment_invalidMediaUrl_throwsBeforeTouchingPost() {
        when(mediaUrlValidator.normalize("javascript:alert(1)"))
                .thenThrow(new IllegalArgumentException("Media URL must use http or https"));

        assertThatThrownBy(() -> commentService.createComment(UUID.randomUUID(), UUID.randomUUID(),
                "physics", "javascript:alert(1)", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http or https");

        verify(commentRepository, never()).save(any());
        verifyNoInteractions(postRepository);
    }

    @Test
    void updateComment_nullMediaUrl_leavesExistingImageUntouched() {
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthorId(authorId);
        comment.setContent("old physics");
        comment.setMediaUrl("https://cdn.example.com/keep.png");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        Comment result = commentService.updateComment(authorId, commentId, "new physics", null);

        assertThat(result.getContent()).isEqualTo("new physics");
        assertThat(result.getMediaUrl()).isEqualTo("https://cdn.example.com/keep.png");
        verifyNoInteractions(mediaUrlValidator);
    }

    @Test
    void updateComment_blankMediaUrl_clearsImage() {
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthorId(authorId);
        comment.setContent("physics text");
        comment.setMediaUrl("https://cdn.example.com/old.png");

        when(mediaUrlValidator.normalize("")).thenReturn(null);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        Comment result = commentService.updateComment(authorId, commentId, null, "");

        assertThat(result.getMediaUrl()).isNull();
        assertThat(result.getContent()).isEqualTo("physics text");
    }

    @Test
    void updateComment_clearingLastRemainingField_throwsException() {
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthorId(authorId);
        comment.setContent(null);
        comment.setMediaUrl("https://cdn.example.com/only.png");

        when(mediaUrlValidator.normalize("")).thenReturn(null);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(authorId, commentId, null, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A comment must have text or an image");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_replacingTextWithImage_isAllowed() {
        UUID authorId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthorId(authorId);
        comment.setContent("old physics");
        String url = "https://cdn.example.com/new.png";

        when(mediaUrlValidator.normalize(url)).thenReturn(url);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        Comment result = commentService.updateComment(authorId, commentId, "", url);

        assertThat(result.getContent()).isNull();
        assertThat(result.getMediaUrl()).isEqualTo(url);
    }

    @Test
    void createComment_notifiesThePostAuthorAndTheRepliedToCommenterOnce() {
        NotificationService notifications = org.mockito.Mockito.mock(NotificationService.class);
        ReflectionTestUtils.setField(commentService, "notificationService", notifications);
        UUID postAuthor = UUID.randomUUID();
        UUID parentAuthor = UUID.randomUUID();
        UUID replier = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(postAuthor);
        Comment parent = new Comment();
        parent.setId(UUID.randomUUID());
        parent.setAuthorId(parentAuthor);
        parent.setPostId(postId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(commentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

        Comment reply = commentService.createComment(replier, postId, "Good point", null, parent.getId());

        org.mockito.Mockito.verify(notifications).notify(postAuthor, replier, NotificationType.POST_COMMENT,
                "COMMENT", reply.getId(), postId, "Good point");
        org.mockito.Mockito.verify(notifications).notify(parentAuthor, replier, NotificationType.COMMENT_REPLY,
                "COMMENT", reply.getId(), postId, "Good point");
    }

    @Test
    void createComment_replyingOnYourOwnPostThreadDoesNotNotifyThePostAuthorTwice() {
        NotificationService notifications = org.mockito.Mockito.mock(NotificationService.class);
        ReflectionTestUtils.setField(commentService, "notificationService", notifications);
        UUID postAuthor = UUID.randomUUID();
        UUID replier = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(postAuthor);
        Comment parent = new Comment();
        parent.setId(UUID.randomUUID());
        parent.setAuthorId(postAuthor);
        parent.setPostId(postId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(commentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

        commentService.createComment(replier, postId, "Thanks!", null, parent.getId());

        org.mockito.Mockito.verify(notifications).notify(any(), any(), any(), any(), any(), any(), any());
    }
}
