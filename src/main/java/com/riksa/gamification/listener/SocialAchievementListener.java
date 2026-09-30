package com.riksa.gamification.listener;

import com.riksa.blog.event.BlogPostApprovedEvent;
import com.riksa.blog.event.BlogPostLikedEvent;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.repository.BlogLikeRepository;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.gamification.service.AchievementService;
import com.riksa.social.event.PostCreatedEvent;
import com.riksa.social.event.ReactionReceivedEvent;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for social and blog events and checks whether any
 * social/blog achievements should be unlocked.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialAchievementListener {

    private final AchievementService achievementService;
    private final PostRepository postRepository;
    private final ReactionRepository reactionRepository;
    private final BlogPostRepository blogPostRepository;
    private final BlogLikeRepository blogLikeRepository;

    @EventListener
    public void handlePostCreated(PostCreatedEvent event) {
        log.info("Checking post achievements for user {}", event.getAuthorId());
        try {
            long postCount = postRepository.countByAuthorId(event.getAuthorId());
            achievementService.checkPostAchievements(event.getAuthorId(), (int) postCount);
        } catch (Exception e) {
            log.error("Failed to check post achievements for user {}: {}",
                    event.getAuthorId(), e.getMessage(), e);
        }
    }

    @EventListener
    public void handleReactionReceived(ReactionReceivedEvent event) {
        log.info("Checking reaction achievements for user {}", event.getPostOwnerId());
        try {
            long totalReactions = reactionRepository.countReactionsOnUserPosts(event.getPostOwnerId());
            achievementService.checkReactionAchievements(event.getPostOwnerId(), (int) totalReactions);
        } catch (Exception e) {
            log.error("Failed to check reaction achievements for user {}: {}",
                    event.getPostOwnerId(), e.getMessage(), e);
        }
    }

    @EventListener
    public void handleBlogApproved(BlogPostApprovedEvent event) {
        log.info("Checking blog post achievements for user {}", event.getAuthorId());
        try {
            long approvedCount = blogPostRepository.countByAuthorIdAndStatus(event.getAuthorId(), BlogStatus.APPROVED);
            achievementService.checkBlogPostAchievements(event.getAuthorId(), (int) approvedCount);
        } catch (Exception e) {
            log.error("Failed to check blog post achievements for user {}: {}",
                    event.getAuthorId(), e.getMessage(), e);
        }
    }

    @EventListener
    public void handleBlogLiked(BlogPostLikedEvent event) {
        log.info("Checking blog like achievements for user {}", event.getAuthorId());
        try {
            long totalLikes = blogLikeRepository.countLikesOnUserBlogPosts(event.getAuthorId());
            achievementService.checkBlogLikeAchievements(event.getAuthorId(), (int) totalLikes);
        } catch (Exception e) {
            log.error("Failed to check blog like achievements for user {}: {}",
                    event.getAuthorId(), e.getMessage(), e);
        }
    }
}