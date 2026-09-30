package com.riksa.auth.event;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class UserProfileUpdatedEvent extends DomainEvent {

    private final UUID userId;

    public UserProfileUpdatedEvent(Object source, UUID userId) {
        super(source);
        this.userId = userId;
    }
}
