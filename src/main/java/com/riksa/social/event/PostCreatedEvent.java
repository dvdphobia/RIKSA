package com.riksa.social.event;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class PostCreatedEvent extends DomainEvent {

    private final UUID postId;
    private final UUID authorId;
    private final UUID categoryId;

    public PostCreatedEvent(Object source, UUID postId, UUID authorId, UUID categoryId) {
        super(source);
        this.postId = postId;
        this.authorId = authorId;
        this.categoryId = categoryId;
    }
}