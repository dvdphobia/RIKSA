package com.riksa.admin.service;

import com.riksa.admin.dto.ContentModerationResponse;
import com.riksa.common.audit.AuditLogService;
import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.entity.PostMedia;
import com.riksa.social.entity.PostMediaId;
import com.riksa.social.enums.ContentStatus;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostMediaRepository;
import com.riksa.social.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialModerationServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private PostMediaRepository postMediaRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private SocialModerationService moderationService;

    private final UUID moderatorId = UUID.randomUUID();

    @Test
    void removePostMediaDetachesEveryMediaRowAndAuditsTheReason() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setMediaUrl("https://media.example.test/first.png");
        List<PostMedia> media = java.util.List.of(
                new PostMedia(new PostMediaId(postId, (short) 0), post.getMediaUrl(), 100, 100),
                new PostMedia(new PostMediaId(postId, (short) 1), "https://media.example.test/second.png", 200, 200));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postMediaRepository.findAllByPostIds(java.util.List.of(postId))).thenReturn(media);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.removePostMedia(moderatorId, postId, "copyright report");

        assertThat(post.getMediaUrl()).isNull();
        verify(postMediaRepository).deleteByPostId(postId);
        verify(auditLogService).record(eq(moderatorId), eq("POST_MEDIA_REMOVE"), eq("POST"), eq(postId),
                eq(Map.of("reason", "copyright report", "removedMediaCount", 2)));
    }

    @Test
    void removeCommentMediaClearsUrlAndAuditsTheReason() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setMediaUrl("https://media.example.test/comment.png");
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.removeCommentMedia(moderatorId, commentId, "privacy request");

        assertThat(comment.getMediaUrl()).isNull();
        verify(auditLogService).record(eq(moderatorId), eq("COMMENT_MEDIA_REMOVE"), eq("COMMENT"), eq(commentId),
                eq(Map.of("reason", "privacy request", "hadMedia", true)));
    }

    @Test
    void hidePost_validRequest_setsFieldsAndAudits() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentModerationResponse response = moderationService.setPostStatus(
                moderatorId, postId, ContentStatus.HIDDEN, "spam");

        assertThat(post.getStatus()).isEqualTo(ContentStatus.HIDDEN);
        assertThat(post.getHiddenReason()).isEqualTo("spam");
        assertThat(post.getHiddenBy()).isEqualTo(moderatorId);
        assertThat(post.getHiddenAt()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("HIDDEN");
        assertThat(response.getReason()).isEqualTo("spam");

        verify(auditLogService).record(eq(moderatorId), eq("POST_HIDE"), eq("POST"), eq(postId),
                eq(Map.of("reason", "spam")));
    }

    @Test
    void listPosts_mapsAnImageOnlyPostWithAnEmptyTitle() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setTitle("");
        post.setContent("");
        post.setMediaUrl("https://api.example.test/api/v1/media/image.png");
        post.setCreatedAt(java.time.OffsetDateTime.now());
        when(postRepository.findByStatus(eq(ContentStatus.VISIBLE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(post), PageRequest.of(0, 10), 1));

        var page = moderationService.listPosts(ContentStatus.VISIBLE, 0, 10);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().id()).isEqualTo(postId);
        assertThat(page.getContent().getFirst().title()).isEmpty();
    }

    @Test
    void hidePost_blankReason_throwsException() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> moderationService.setPostStatus(moderatorId, postId, ContentStatus.HIDDEN, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reason is required to hide content");

        assertThat(post.getStatus()).isEqualTo(ContentStatus.VISIBLE);
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void hidePost_alreadyHidden_throwsException() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setStatus(ContentStatus.HIDDEN);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> moderationService.setPostStatus(moderatorId, postId, ContentStatus.HIDDEN, "spam"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Post is already HIDDEN");
    }

    @Test
    void restorePost_clearsFieldsAndAudits() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setStatus(ContentStatus.HIDDEN);
        post.setHiddenReason("spam");
        post.setHiddenBy(UUID.randomUUID());

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        ContentModerationResponse response = moderationService.setPostStatus(
                moderatorId, postId, ContentStatus.VISIBLE, null);

        assertThat(post.getStatus()).isEqualTo(ContentStatus.VISIBLE);
        assertThat(post.getHiddenReason()).isNull();
        assertThat(post.getHiddenBy()).isNull();
        assertThat(post.getHiddenAt()).isNull();
        assertThat(response.getStatus()).isEqualTo("VISIBLE");

        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).record(eq(moderatorId), eq("POST_RESTORE"), eq("POST"), eq(postId), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue()).containsEntry("previousReason", "spam");
    }

    @Test
    void restorePost_visiblePost_throwsException() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> moderationService.setPostStatus(moderatorId, postId, ContentStatus.VISIBLE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Post is already VISIBLE");
    }

    @Test
    void setPostStatus_nonExistentPost_throwsException() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moderationService.setPostStatus(moderatorId, postId, ContentStatus.HIDDEN, "spam"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");
    }

    @Test
    void hideComment_setsFieldsDecrementsCountAndAudits() {
        UUID commentId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setPostId(postId);
        Post post = new Post();
        post.setId(postId);
        post.setCommentCount(5);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.setCommentStatus(moderatorId, commentId, ContentStatus.HIDDEN, "harassment");

        assertThat(comment.getStatus()).isEqualTo(ContentStatus.HIDDEN);
        assertThat(comment.getHiddenReason()).isEqualTo("harassment");
        assertThat(post.getCommentCount()).isEqualTo(4);

        verify(auditLogService).record(eq(moderatorId), eq("COMMENT_HIDE"), eq("COMMENT"), eq(commentId),
                eq(Map.of("reason", "harassment")));
    }

    @Test
    void restoreComment_incrementsCountAndAudits() {
        UUID commentId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setPostId(postId);
        comment.setStatus(ContentStatus.HIDDEN);
        comment.setHiddenReason("harassment");
        Post post = new Post();
        post.setId(postId);
        post.setCommentCount(4);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        moderationService.setCommentStatus(moderatorId, commentId, ContentStatus.VISIBLE, null);

        assertThat(comment.getStatus()).isEqualTo(ContentStatus.VISIBLE);
        assertThat(comment.getHiddenReason()).isNull();
        assertThat(post.getCommentCount()).isEqualTo(5);

        verify(auditLogService).record(eq(moderatorId), eq("COMMENT_RESTORE"), eq("COMMENT"), eq(commentId),
                eq(Map.of("previousReason", "harassment")));
    }

    @Test
    void hideComment_alreadyHidden_throwsException() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setStatus(ContentStatus.HIDDEN);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> moderationService.setCommentStatus(moderatorId, commentId, ContentStatus.HIDDEN, "spam"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Comment is already HIDDEN");
    }

    @Test
    void restoreComment_notHidden_throwsException() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> moderationService.setCommentStatus(moderatorId, commentId, ContentStatus.VISIBLE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Comment is already VISIBLE");
    }

    @Test
    void setCommentStatus_nonExistentComment_throwsException() {
        UUID commentId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moderationService.setCommentStatus(moderatorId, commentId, ContentStatus.HIDDEN, "spam"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Comment not found");
    }
}
