package com.riksa.blog.event;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

import java.util.UUID;

/**
 * Published when a blog post is approved by a moderator.
 * AchievementService listens to this to check WRITE_BLOG_POST achievements.
 */
@Getter
public class BlogPostApprovedEvent extends DomainEvent {

    private final UUID blogPostId;
    private final UUID authorId;

    public BlogPostApprovedEvent(Object source, UUID blogPostId, UUID authorId) {
        super(source);
        this.blogPostId = blogPostId;
        this.authorId = authorId;
    }
}