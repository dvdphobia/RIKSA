package com.riksa.social.service;

import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.entity.Reaction;
import com.riksa.social.enums.ContentStatus;
import com.riksa.social.enums.ReactionType;
import com.riksa.social.event.ReactionReceivedEvent;
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

    @Transactional
    public Post react(UUID userId, UUID postId, UUID commentId, ReactionType type) {
        rejectHiddenTargets(postId, commentId);

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

            // Notify achievement system that the post owner received a reaction
            if (postId != null) {
                Post post = getPost(postId);
                eventPublisher.publishEvent(new ReactionReceivedEvent(this, postId, post.getAuthorId()));
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
        reactionRepository.findByUserIdAndPostIdAndCommentId(userId, postId, commentId)
                .ifPresent(reactionRepository::delete);
        return adjustPostCounter(postId, -1);
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