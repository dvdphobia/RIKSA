package com.riksa.social.service;

import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.enums.ContentStatus;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import com.riksa.quest.event.SocialActionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MediaUrlValidator mediaUrlValidator;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    /**
     * A comment may be text, an image, or both — but not neither.
     */
    @Transactional
    public Comment createComment(UUID authorId, UUID postId, String content, String mediaUrl, UUID parentId) {
        String normalizedMediaUrl = mediaUrlValidator.normalize(mediaUrl);
        String normalizedContent = blankToNull(content);

        requirePresent(normalizedContent, normalizedMediaUrl);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
        if (post.getStatus() == ContentStatus.HIDDEN) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }

        Comment comment = new Comment();
        comment.setAuthorId(authorId);
        comment.setPostId(postId);
        comment.setContent(normalizedContent);
        comment.setMediaUrl(normalizedMediaUrl);
        comment.setParentId(parentId);

        Comment saved = commentRepository.save(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        if (eventPublisher != null && post.getAuthorId() != null && !post.getAuthorId().equals(authorId)) {
            eventPublisher.publishEvent(new SocialActionEvent(this, authorId));
        }

        return saved;
    }

    /**
     * {@code null} leaves a field unchanged; a blank string clears it. The
     * resulting comment must still have text or an image.
     */
    @Transactional
    public Comment updateComment(UUID authorId, UUID commentId, String content, String mediaUrl) {
        Comment comment = getComment(commentId);

        if (!comment.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("You can only edit your own comments");
        }

        String nextContent = comment.getContent();
        if (content != null) {
            nextContent = blankToNull(content);
        }

        String nextMediaUrl = comment.getMediaUrl();
        if (mediaUrl != null) {
            nextMediaUrl = mediaUrlValidator.normalize(mediaUrl);
        }

        requirePresent(nextContent, nextMediaUrl);

        comment.setContent(nextContent);
        comment.setMediaUrl(nextMediaUrl);
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

    private void requirePresent(String content, String mediaUrl) {
        if (content == null && mediaUrl == null) {
            throw new IllegalArgumentException("A comment must have text or an image");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}


