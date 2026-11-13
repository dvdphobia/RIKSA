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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
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
        if (type == null) {
            throw new IllegalArgumentException("Reaction type is required");
        }
        Post post = lockVisibleTarget(postId, commentId);

        List<Reaction> existing = reactionRepository
                .findAllByUserIdAndPostIdAndCommentIdOrderByCreatedAtAscIdAsc(userId, postId, commentId);

        if (!existing.isEmpty()) {
            boolean togglesOff = existing.stream().allMatch(reaction -> reaction.getType() == type);
            if (togglesOff) {
                // same type → toggle off (remove)
                reactionRepository.deleteAll(existing);
                Post updatedPost = adjustPostCounter(post, postId == null ? 0 : -existing.size());
                announceChange(post.getId());
                return updatedPost;
            } else {
                Reaction canonical = existing.getFirst();
                canonical.setType(type);
                reactionRepository.save(canonical);
                if (existing.size() > 1) {
                    reactionRepository.deleteAll(existing.subList(1, existing.size()));
                }
                Post updatedPost = existing.size() > 1
                        ? adjustPostCounter(post, postId == null ? 0 : -(existing.size() - 1))
                        : post;
                announceChange(post.getId());
                return updatedPost;
            }
        }

        Reaction reaction = new Reaction();
        reaction.setUserId(userId);
        reaction.setPostId(postId);
        reaction.setCommentId(commentId);
        reaction.setType(type);
        reactionRepository.save(reaction);
        Post updatedPost = adjustPostCounter(post, postId == null ? 0 : +1);

        // Preserve the achievement event and count one action on another learner's content.
        notifyReaction(userId, post, postId, commentId);
        if (postId != null) {
            eventPublisher.publishEvent(new ReactionReceivedEvent(this, postId, post.getAuthorId()));
        }
        if (post.getAuthorId() != null && !post.getAuthorId().equals(userId)) {
            eventPublisher.publishEvent(new SocialActionEvent(this, userId));
        }

        announceChange(post.getId());
        return updatedPost;
    }

    @Transactional
    public Post removeReaction(UUID userId, UUID postId, UUID commentId) {
        Post post = lockVisibleTarget(postId, commentId);
        List<Reaction> existing = reactionRepository
                .findAllByUserIdAndPostIdAndCommentIdOrderByCreatedAtAscIdAsc(userId, postId, commentId);
        if (existing.isEmpty()) {
            return post;
        }

        reactionRepository.deleteAll(existing);
        Post updatedPost = adjustPostCounter(post, postId == null ? 0 : -existing.size());
        announceChange(post.getId());
        return updatedPost;
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
    private void announceChange(UUID postId) {
        eventPublisher.publishEvent(RealtimeEvent.toEveryone(
                this, RealtimeTypes.POST_CHANGED, Map.of("postId", postId)));
    }

    private Post adjustPostCounter(Post post, int delta) {
        if (delta == 0) {
            return post;
        }
        post.setReactionCount(Math.max(0, post.getReactionCount() + delta));
        return postRepository.save(post);
    }

    private Post lockVisibleTarget(UUID postId, UUID commentId) {
        if (commentId != null) {
            Comment comment = commentRepository.findByIdForUpdate(commentId)
                    .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
            if (comment.getStatus() == ContentStatus.HIDDEN) {
                throw new IllegalArgumentException("Comment not found: " + commentId);
            }
            if (postId != null && !Objects.equals(postId, comment.getPostId())) {
                throw new IllegalArgumentException("Comment does not belong to the specified post");
            }
            postId = comment.getPostId();
        }
        if (postId == null) {
            throw new IllegalArgumentException("Post or comment target is required");
        }
        UUID resolvedPostId = postId;
        Post post = postRepository.findByIdForUpdate(resolvedPostId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + resolvedPostId));
        if (post.getStatus() == ContentStatus.HIDDEN) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }
        return post;
    }
}

