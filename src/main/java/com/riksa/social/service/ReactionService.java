package com.riksa.social.service;

import com.riksa.notification.entity.NotificationType;
import com.riksa.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import com.riksa.realtime.RealtimeEvent;
import com.riksa.realtime.RealtimeTypes;
import java.util.Map;
import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.entity.Reaction;
import com.riksa.social.enums.ContentStatus;
import com.riksa.social.enums.ReactionType;
import com.riksa.social.event.ReactionReceivedEvent;
import com.riksa.quest.event.SocialActionEvent;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** Optional so hand-built services in tests keep working. */
    @Autowired(required = false)
    private NotificationService notificationService;

    @Transactional
    public Post react(UUID userId, UUID postId, UUID commentId, ReactionType type) {
        rejectHiddenTargets(postId, commentId);
        announceChange(postId, commentId);

        Optional<Reaction> existing = reactionRepository
                .findByUserIdAndPostIdAndCommentId(userId, postId, commentId);

        if (existing.isPresent()) {
            Reaction reaction = existing.get();
            if (reaction.getType() == type) {
                // same type → toggle off (remove)
                reactionRepository.delete(reaction);
                return adjustPostCounter(postId, -1);
            } else {
                reaction.setType(type);
                reactionRepository.save(reaction);
                return getPost(postId);
            }
        }

        try {
            Reaction reaction = new Reaction();
            reaction.setUserId(userId);
            reaction.setPostId(postId);
            reaction.setCommentId(commentId);
            reaction.setType(type);
            reactionRepository.save(reaction);
            Post updatedPost = adjustPostCounter(postId, +1);

            // Preserve the achievement event and count one action on another learner's content.
            UUID actionPostId = postId;
            if (actionPostId == null && commentId != null) {
                actionPostId = commentRepository.findById(commentId).map(Comment::getPostId).orElse(null);
            }
            if (actionPostId != null) {
                Post post = getPost(actionPostId);
                notifyReaction(userId, post, postId, commentId);
                if (postId != null) {
                    eventPublisher.publishEvent(new ReactionReceivedEvent(this, postId, post.getAuthorId()));
                }
                if (post.getAuthorId() != null && !post.getAuthorId().equals(userId)) {
                    eventPublisher.publishEvent(new SocialActionEvent(this, userId));
                }
            }

            return updatedPost;
        } catch (DataIntegrityViolationException e) {
            Reaction reaction = reactionRepository
                    .findByUserIdAndPostIdAndCommentId(userId, postId, commentId)
                    .orElseThrow();
            reaction.setType(type);
            reactionRepository.save(reaction);
            return getPost(postId);
        }
    }

    @Transactional
    public Post removeReaction(UUID userId, UUID postId, UUID commentId) {
        announceChange(postId, commentId);
        reactionRepository.findByUserIdAndPostIdAndCommentId(userId, postId, commentId)
                .ifPresent(reactionRepository::delete);
        return adjustPostCounter(postId, -1);
    }

    /** A new reaction tells the owner of the post or comment. Changes and removals don't. */
    private void notifyReaction(UUID userId, Post post, UUID postId, UUID commentId) {
        if (notificationService == null) return;
        if (postId != null) {
            notificationService.notify(post.getAuthorId(), userId, NotificationType.POST_REACTION,
                    "POST", post.getId(), post.getId(), postTeaser(post));
        } else {
            commentRepository.findById(commentId).ifPresent(comment -> notificationService.notify(
                    comment.getAuthorId(), userId, NotificationType.COMMENT_REACTION,
                    "COMMENT", comment.getId(), post.getId(), comment.getContent()));
        }
    }

    private static String postTeaser(Post post) {
        return post.getTitle() != null && !post.getTitle().isBlank() ? post.getTitle() : post.getContent();
    }

    /** Delivered only if the transaction commits, so every return path is covered. */
    private void announceChange(UUID postId, UUID commentId) {
        UUID changedPostId = postId;
        if (changedPostId == null && commentId != null) {
            changedPostId = commentRepository.findById(commentId).map(Comment::getPostId).orElse(null);
        }
        if (changedPostId != null) {
            eventPublisher.publishEvent(RealtimeEvent.toEveryone(
                    this, RealtimeTypes.POST_CHANGED, Map.of("postId", changedPostId)));
        }
    }

    private Post adjustPostCounter(UUID postId, int delta) {
        if (postId == null) return null;
        Post post = getPost(postId);
        post.setReactionCount(Math.max(0, post.getReactionCount() + delta));
        return postRepository.save(post);
    }

    private Post getPost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
    }

    private void rejectHiddenTargets(UUID postId, UUID commentId) {
        if (postId != null && getPost(postId).getStatus() == ContentStatus.HIDDEN) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }
        if (commentId != null) {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
            if (comment.getStatus() == ContentStatus.HIDDEN) {
                throw new IllegalArgumentException("Comment not found: " + commentId);
            }
        }
    }
}

