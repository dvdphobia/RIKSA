package com.riksa.common.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around Spring's ApplicationEventPublisher.
 * Centralises all event publishing so callers don't need to inject
 * the Spring interface directly and we can add logging/metrics later.
 */
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Publish a domain event. Listeners in any domain can subscribe.
     * Event delivery is synchronous by default; add @Async on listeners
     * that need fire-and-forget semantics.
     */
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}