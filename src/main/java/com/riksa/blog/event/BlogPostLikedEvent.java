package com.riksa.blog.event;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

import java.util.UUID;

/**
 * Published when a user likes a blog post.
 * AchievementService listens to this to check RECEIVE_BLOG_LIKE achievements
 * for the blog post's author.
 */
@Getter
public class BlogPostLikedEvent extends DomainEvent {

    private final UUID blogPostId;
    private final UUID authorId;
    private final UUID likerId;

    public BlogPostLikedEvent(Object source, UUID blogPostId, UUID authorId, UUID likerId) {
        super(source);
        this.blogPostId = blogPostId;
        this.authorId = authorId;
        this.likerId = likerId;
    }
}