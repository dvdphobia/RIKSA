package com.riksa.social.service;

import com.riksa.notification.entity.NotificationType;
import com.riksa.notification.service.NotificationService;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.moderation.ContentModerationService;
import com.riksa.moderation.AuthorSlidingWindowRateLimiter;
import com.riksa.moderation.ModerationInput;
import com.riksa.moderation.ModerationReasonCode;
import com.riksa.moderation.ModerationResult;
import com.riksa.moderation.ModerationSurface;
import com.riksa.moderation.RateLimitedException;
import com.riksa.realtime.RealtimeEvent;
import com.riksa.realtime.RealtimeTypes;
import java.util.Map;
import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.enums.ContentStatus;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import com.riksa.quest.event.SocialActionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);
    private static final int COMMENT_RATE_LIMIT = 10;
    private static final int COMMENT_EDIT_RATE_LIMIT = 10;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MediaUrlValidator mediaUrlValidator;
    private final UserRepository userRepository;
    private final ContentModerationService contentModerationService;
    private final AuthorSlidingWindowRateLimiter editRateLimiter;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private CommunityModerationService communityModeration;

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
        if (communityModeration != null) {
            communityModeration.requireNotBanned(authorId, post.getCategoryId());
        }
        ModerationResult moderation = screenComment(authorId, normalizedContent, null, null);

        Comment comment = new Comment();
        comment.setAuthorId(authorId);
        comment.setPostId(postId);
        comment.setContent(normalizedContent);
        comment.setMediaUrl(normalizedMediaUrl);
        comment.setParentId(parentId);

        Comment saved = commentRepository.save(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
        logReview(moderation, saved.getId());
        announceChange(postId);

        if (eventPublisher != null && post.getAuthorId() != null && !post.getAuthorId().equals(authorId)) {
            eventPublisher.publishEvent(new SocialActionEvent(this, authorId));
        }
        notifyComment(authorId, post, saved);

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
        editRateLimiter.checkAndRecord("social-comment-edit", authorId,
                COMMENT_EDIT_RATE_LIMIT, Duration.ofMinutes(10));
        ModerationResult moderation = screenComment(
                authorId, nextContent, comment.getId(), comment.getCreatedAt());

        comment.setContent(nextContent);
        comment.setMediaUrl(nextMediaUrl);
        Comment saved = commentRepository.save(comment);
        logReview(moderation, saved.getId());
        return saved;
    }

    @Transactional
    public void deleteComment(UUID authorId, UUID commentId) {
        Comment comment = getComment(commentId);

        if (!comment.getAuthorId().equals(authorId) && !moderatesPostOf(authorId, comment)) {
            throw new IllegalStateException("You can only delete your own comments");
        }

        commentRepository.delete(comment);

        postRepository.findById(comment.getPostId()).ifPresent(post -> {
            post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
            postRepository.save(post);
        });
        announceChange(comment.getPostId());
    }

    /** The post's author hears about every comment; a reply also tells the parent's author. */
    private void notifyComment(UUID authorId, Post post, Comment comment) {
        if (notificationService == null) return;
        String preview = comment.getContent();
        notificationService.notify(post.getAuthorId(), authorId, NotificationType.POST_COMMENT,
                "COMMENT", comment.getId(), post.getId(), preview);
        if (comment.getParentId() != null) {
            commentRepository.findById(comment.getParentId())
                    .filter(parent -> !parent.getAuthorId().equals(post.getAuthorId()))
                    .ifPresent(parent -> notificationService.notify(parent.getAuthorId(), authorId,
                            NotificationType.COMMENT_REPLY, "COMMENT", comment.getId(), post.getId(), preview));
        }
    }

    private void announceChange(UUID postId) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(RealtimeEvent.toEveryone(
                    this, RealtimeTypes.POST_CHANGED, Map.of("postId", postId)));
        }
    }

    /** Community owners and moderators may remove any comment in their community. */
    private boolean moderatesPostOf(UUID userId, Comment comment) {
        if (communityModeration == null) return false;
        return postRepository.findById(comment.getPostId())
                .map(post -> communityModeration.canModerate(userId, post.getCategoryId()))
                .orElse(false);
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

    private ModerationResult screenComment(UUID authorId, String content,
                                           UUID ignoredCommentId, OffsetDateTime ignoredCreatedAt) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime rateCutoff = now.minusMinutes(10);
        long recentCommentCount = commentRepository.countByAuthorIdAndCreatedAtAfter(authorId, rateCutoff);
        if (ignoredCreatedAt != null && ignoredCreatedAt.isAfter(rateCutoff)) {
            recentCommentCount--;
        }
        if (recentCommentCount >= COMMENT_RATE_LIMIT) {
            throw new RateLimitedException();
        }

        OffsetDateTime duplicateCutoff = now.minusHours(24);
        List<Comment> recentComments = commentRepository.findByAuthorIdAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
                authorId, duplicateCutoff, PageRequest.of(0, 50));
        OffsetDateTime accountCreatedAt = userRepository.findById(authorId)
                .map(User::getCreatedAt)
                .orElse(null);
        ModerationResult result = contentModerationService.assess(
                new ModerationInput(authorId, accountCreatedAt, ModerationSurface.COMMENT, null, content),
                duplicateKey -> recentComments.stream()
                        .filter(candidate -> ignoredCommentId == null
                                || !Objects.equals(candidate.getId(), ignoredCommentId))
                        .anyMatch(candidate -> contentModerationService.normalizeForDuplicate(candidate.getContent())
                                .equals(duplicateKey)));
        contentModerationService.requireNotBlocked(result);
        return result;
    }

    private void logReview(ModerationResult result, UUID commentId) {
        if (result.verdict() == com.riksa.moderation.ModerationVerdict.REVIEW) {
            List<ModerationReasonCode> reasonCodes = result.reasons().stream()
                    .map(reason -> reason.code())
                    .toList();
            log.info("Content moderation review surface=COMMENT id={} score={} reasonCodes={}",
                    commentId, result.score(), reasonCodes);
        }
    }
}


