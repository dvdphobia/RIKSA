package com.riksa.admin.service;

import com.riksa.admin.dto.ContentModerationResponse;
import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.enums.ContentStatus;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialModerationService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ContentModerationResponse setPostStatus(UUID moderatorId, UUID postId, ContentStatus newStatus, String reason) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));

        if (post.getStatus() == newStatus) {
            throw new IllegalArgumentException("Post is already " + newStatus);
        }

        if (newStatus == ContentStatus.HIDDEN) {
            requireReason(reason);
            post.setStatus(ContentStatus.HIDDEN);
            post.setHiddenReason(reason);
            post.setHiddenBy(moderatorId);
            post.setHiddenAt(OffsetDateTime.now());
            postRepository.save(post);
            auditLogService.record(moderatorId, "POST_HIDE", "POST", postId, Map.of("reason", reason));
        } else {
            String previousReason = post.getHiddenReason();
            post.setStatus(ContentStatus.VISIBLE);
            post.setHiddenReason(null);
            post.setHiddenBy(null);
            post.setHiddenAt(null);
            postRepository.save(post);
            Map<String, Object> details = new LinkedHashMap<>();
            if (previousReason != null) {
                details.put("previousReason", previousReason);
            }
            auditLogService.record(moderatorId, "POST_RESTORE", "POST", postId, details);
        }

        return toResponse(post.getId(), post.getStatus(), post.getHiddenReason(), post.getHiddenBy(), post.getHiddenAt());
    }

    @Transactional
    public ContentModerationResponse setCommentStatus(UUID moderatorId, UUID commentId, ContentStatus newStatus, String reason) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        if (comment.getStatus() == newStatus) {
            throw new IllegalArgumentException("Comment is already " + newStatus);
        }

        if (newStatus == ContentStatus.HIDDEN) {
            requireReason(reason);
            comment.setStatus(ContentStatus.HIDDEN);
            comment.setHiddenReason(reason);
            comment.setHiddenBy(moderatorId);
            comment.setHiddenAt(OffsetDateTime.now());
            commentRepository.save(comment);
            adjustCommentCount(comment.getPostId(), -1);
            auditLogService.record(moderatorId, "COMMENT_HIDE", "COMMENT", commentId, Map.of("reason", reason));
        } else {
            String previousReason = comment.getHiddenReason();
            comment.setStatus(ContentStatus.VISIBLE);
            comment.setHiddenReason(null);
            comment.setHiddenBy(null);
            comment.setHiddenAt(null);
            commentRepository.save(comment);
            adjustCommentCount(comment.getPostId(), +1);
            Map<String, Object> details = new LinkedHashMap<>();
            if (previousReason != null) {
                details.put("previousReason", previousReason);
            }
            auditLogService.record(moderatorId, "COMMENT_RESTORE", "COMMENT", commentId, details);
        }

        return toResponse(comment.getId(), comment.getStatus(), comment.getHiddenReason(), comment.getHiddenBy(), comment.getHiddenAt());
    }

    private void adjustCommentCount(UUID postId, int delta) {
        postRepository.findById(postId).ifPresent(post -> {
            post.setCommentCount(Math.max(0, post.getCommentCount() + delta));
            postRepository.save(post);
        });
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason is required to hide content");
        }
    }

    private ContentModerationResponse toResponse(UUID id, ContentStatus status, String reason, UUID hiddenBy, OffsetDateTime hiddenAt) {
        return new ContentModerationResponse(id, status.name(), reason, hiddenBy, hiddenAt);
    }
}
