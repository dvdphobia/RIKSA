package com.riksa.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * Base class for all domain events. Extends Spring's ApplicationEvent
 * so that listeners can use @EventListener.
 * All cross-domain communication uses events — never direct writes to
 * another domain's tables.
 */
public abstract class DomainEvent extends ApplicationEvent {

    protected DomainEvent(Object source) {
        super(source);
    }
}