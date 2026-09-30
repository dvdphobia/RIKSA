package com.riksa.blog.event;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

import java.util.UUID;

/**
 * Published when a blog post is approved by a moderator.
 * AI context listener can consume this to update the user's context.
 */
@Getter
public class BlogApprovedEvent extends DomainEvent {

    private final UUID postId;
    private final UUID authorId;
    private final UUID moderatorId;

    public BlogApprovedEvent(Object source, UUID postId, UUID authorId, UUID moderatorId) {
        super(source);
        this.postId = postId;
        this.authorId = authorId;
        this.moderatorId = moderatorId;
    }
}