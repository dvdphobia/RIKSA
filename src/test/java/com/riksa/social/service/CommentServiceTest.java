package com.riksa.social.service;

import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
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
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private ContentFilterService contentFilterService;

    @InjectMocks private CommentService commentService;

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

        Comment result = commentService.createComment(authorId, postId, "Great physics explanation", null);

        assertThat(result).isNotNull();
        assertThat(result.getAuthorId()).isEqualTo(authorId);
        assertThat(result.getPostId()).isEqualTo(postId);
        assertThat(post.getCommentCount()).isEqualTo(6);
    }

    @Test
    void createComment_nonStemContent_throwsException() {
        doThrow(new IllegalArgumentException("Content must be related to STEM, Tech, or History"))
                .when(contentFilterService).validate(anyString());

        assertThatThrownBy(() -> commentService.createComment(UUID.randomUUID(), UUID.randomUUID(),
                "Hello world", null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(commentRepository, never()).save(any());
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

        Comment result = commentService.updateComment(authorId, commentId, "Updated physics content");

        assertThat(result.getContent()).isEqualTo("Updated physics content");
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

        assertThatThrownBy(() -> commentService.updateComment(authorId, commentId, "new content"))
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
}