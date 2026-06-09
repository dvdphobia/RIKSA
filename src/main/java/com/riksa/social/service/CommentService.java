package com.riksa.social.service;

import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ContentFilterService contentFilterService;

    @Transactional
    public Comment createComment(UUID authorId, UUID postId, String content, UUID parentId) {
        contentFilterService.validate(content);

        Comment comment = new Comment();
        comment.setAuthorId(authorId);
        comment.setPostId(postId);
        comment.setContent(content);
        comment.setParentId(parentId);

        Comment saved = commentRepository.save(comment);

        postRepository.findById(postId).ifPresent(post -> {
            post.setCommentCount(post.getCommentCount() + 1);
            postRepository.save(post);
        });

        return saved;
    }

    @Transactional
    public Comment updateComment(UUID authorId, UUID commentId, String content) {
        Comment comment = getComment(commentId);

        if (!comment.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("You can only edit your own comments");
        }

        contentFilterService.validate(content);
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(UUID authorId, UUID commentId) {
        Comment comment = getComment(commentId);

        if (!comment.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("You can only delete your own comments");
        }

        commentRepository.delete(comment);

        postRepository.findById(comment.getPostId()).ifPresent(post -> {
            post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
            postRepository.save(post);
        });
    }

    public Comment getComment(UUID commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
    }
}