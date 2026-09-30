package com.riksa.realtime;

import com.riksa.common.event.DomainEvent;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

/**
 * A small "this changed" message for connected apps. It carries ids, never
 * content: the app refetches what it shows through the normal REST API, so
 * visibility rules stay in one place.
 *
 * <p>Published like any domain event and delivered after the transaction
 * commits, so an app that refetches on receipt sees the change.
 */
@Getter
public class RealtimeEvent extends DomainEvent {

    /** Who receives it; {@code null} sends it to every connected learner. */
    private final UUID recipientId;
    private final String type;
    private final Map<String, Object> data;

    private RealtimeEvent(Object source, UUID recipientId, String type, Map<String, Object> data) {
        super(source);
        this.recipientId = recipientId;
        this.type = type;
        this.data = data;
    }

    public static RealtimeEvent toEveryone(Object source, String type, Map<String, Object> data) {
        return new RealtimeEvent(source, null, type, data);
    }

    public static RealtimeEvent toUser(Object source, UUID userId, String type, Map<String, Object> data) {
        return new RealtimeEvent(source, userId, type, data);
    }
}
