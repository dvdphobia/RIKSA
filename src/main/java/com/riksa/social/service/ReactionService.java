package com.riksa.social.service;

import com.riksa.social.entity.Post;
import com.riksa.social.entity.Reaction;
import com.riksa.social.enums.ReactionType;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public Post react(UUID userId, UUID postId, UUID commentId, ReactionType type) {
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
            return adjustPostCounter(postId, +1);
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
}