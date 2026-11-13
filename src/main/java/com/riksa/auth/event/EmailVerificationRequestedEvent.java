package com.riksa.auth.event;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

/** In-memory request to send a persisted email verification token after commit. */
@Getter
public class EmailVerificationRequestedEvent extends DomainEvent {

    private final String email;
    private final String token;

    public EmailVerificationRequestedEvent(Object source, String email, String token) {
        super(source);
        this.email = email;
        this.token = token;
    }
}
