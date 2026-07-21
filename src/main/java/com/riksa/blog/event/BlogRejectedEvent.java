package com.riksa.blog.event;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

import java.util.UUID;

/**
 * Published when a blog post is rejected by a moderator.
 * AI context listener can consume this to update the user's context.
 */
@Getter
public class BlogRejectedEvent extends DomainEvent {

    private final UUID postId;
    private final UUID authorId;
    private final UUID moderatorId;
    private final String reason;

    public BlogRejectedEvent(Object source, UUID postId, UUID authorId, UUID moderatorId, String reason) {
        super(source);
        this.postId = postId;
        this.authorId = authorId;
        this.moderatorId = moderatorId;
        this.reason = reason;
    }
}