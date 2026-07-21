package com.riksa.social.event;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

import java.util.UUID;

/**
 * Published when a user receives a reaction on their post.
 * AchievementService listens to this to check RECEIVE_REACTION achievements.
 */
@Getter
public class ReactionReceivedEvent extends DomainEvent {

    private final UUID postId;
    private final UUID postOwnerId;

    public ReactionReceivedEvent(Object source, UUID postId, UUID postOwnerId) {
        super(source);
        this.postId = postId;
        this.postOwnerId = postOwnerId;
    }
}