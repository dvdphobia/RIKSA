package com.riksa.social.service;

import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.entity.Reaction;
import com.riksa.social.enums.ContentStatus;
import com.riksa.social.enums.ReactionType;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.ReactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @Mock private ReactionRepository reactionRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private ReactionService reactionService;

    @Test
    void react_newReaction_incrementsCounter() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(UUID.randomUUID());
        post.setReactionCount(0);

        when(reactionRepository.findAllByUserIdAndPostIdAndCommentIdOrderByCreatedAtAscIdAsc(userId, postId, null))
                .thenReturn(List.of());
        when(postRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post result = reactionService.react(userId, postId, null, ReactionType.UPVOTE);

        assertThat(result.getReactionCount()).isEqualTo(1);
        verify(reactionRepository).save(any(Reaction.class));
    }

    @Test
    void react_sameTypeSameReaction_togglesOff() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setReactionCount(3);

        Reaction existing = new Reaction();
        existing.setUserId(userId);
        existing.setPostId(postId);
        existing.setType(ReactionType.UPVOTE);

        when(reactionRepository.findAllByUserIdAndPostIdAndCommentIdOrderByCreatedAtAscIdAsc(userId, postId, null))
                .thenReturn(List.of(existing));
        when(postRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post result = reactionService.react(userId, postId, null, ReactionType.UPVOTE);

        assertThat(result.getReactionCount()).isEqualTo(2);
        verify(reactionRepository).deleteAll(List.of(existing));
    }

    @Test
    void react_differentType_updatesReaction() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setReactionCount(1);

        Reaction existing = new Reaction();
        existing.setUserId(userId);
        existing.setPostId(postId);
        existing.setType(ReactionType.UPVOTE);

        when(reactionRepository.findAllByUserIdAndPostIdAndCommentIdOrderByCreatedAtAscIdAsc(userId, postId, null))
                .thenReturn(List.of(existing));
        when(postRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));

        Post result = reactionService.react(userId, postId, null, ReactionType.LOVE);

        assertThat(result.getReactionCount()).isEqualTo(1); // count unchanged, type changed
        verify(reactionRepository).save(existing);
        assertThat(existing.getType()).isEqualTo(ReactionType.LOVE);
    }

    @Test
    void removeReaction_existing_decrementsCounter() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setReactionCount(5);

        Reaction existing = new Reaction();
        existing.setUserId(userId);
        existing.setPostId(postId);

        when(reactionRepository.findAllByUserIdAndPostIdAndCommentIdOrderByCreatedAtAscIdAsc(userId, postId, null))
                .thenReturn(List.of(existing));
        when(postRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post result = reactionService.removeReaction(userId, postId, null);

        assertThat(result.getReactionCount()).isEqualTo(4);
        verify(reactionRepository).deleteAll(List.of(existing));
    }

    @Test
    void removeReaction_nonExistent_doesNotAdjustCounter() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setReactionCount(3);

        when(reactionRepository.findAllByUserIdAndPostIdAndCommentIdOrderByCreatedAtAscIdAsc(userId, postId, null))
                .thenReturn(List.of());
        when(postRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));

        Post result = reactionService.removeReaction(userId, postId, null);

        assertThat(result.getReactionCount()).isEqualTo(3);
        verify(postRepository, never()).save(any(Post.class));
        verify(reactionRepository, never()).deleteAll(any());
    }

    @Test
    void react_hiddenPost_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setStatus(ContentStatus.HIDDEN);

        when(postRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> reactionService.react(userId, postId, null, ReactionType.UPVOTE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");

        verify(reactionRepository, never()).save(any(Reaction.class));
    }

    @Test
    void react_hiddenComment_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setStatus(ContentStatus.HIDDEN);

        when(commentRepository.findByIdForUpdate(commentId)).thenReturn(Optional.of(comment));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> reactionService.react(userId, postId, commentId, ReactionType.UPVOTE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Comment not found");

        verify(reactionRepository, never()).save(any(Reaction.class));
    }

    @Test
    void removeReaction_repeatedRequestOnlyDecrementsOnce() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setReactionCount(5);
        Reaction existing = new Reaction();
        existing.setUserId(userId);
        existing.setPostId(postId);

        when(reactionRepository.findAllByUserIdAndPostIdAndCommentIdOrderByCreatedAtAscIdAsc(userId, postId, null))
                .thenReturn(List.of(existing), List.of());
        when(postRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post firstResult = reactionService.removeReaction(userId, postId, null);
        Post secondResult = reactionService.removeReaction(userId, postId, null);

        assertThat(firstResult.getReactionCount()).isEqualTo(4);
        assertThat(secondResult.getReactionCount()).isEqualTo(4);
        verify(reactionRepository, times(1)).deleteAll(List.of(existing));
    }

    @Test
    void react_reactionCountDoesNotGoNegative() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setReactionCount(0);

        Reaction existing = new Reaction();
        existing.setUserId(userId);
        existing.setPostId(postId);
        existing.setType(ReactionType.UPVOTE);

        when(reactionRepository.findAllByUserIdAndPostIdAndCommentIdOrderByCreatedAtAscIdAsc(userId, postId, null))
                .thenReturn(List.of(existing));
        when(postRepository.findByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post result = reactionService.react(userId, postId, null, ReactionType.UPVOTE);

        assertThat(result.getReactionCount()).isEqualTo(0);
    }
}
