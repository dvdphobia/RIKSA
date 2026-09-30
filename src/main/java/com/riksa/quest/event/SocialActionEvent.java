package com.riksa.quest.event;
import com.riksa.common.event.DomainEvent;
import lombok.Getter;
import java.util.UUID;
@Getter
public class SocialActionEvent extends DomainEvent {
    private final UUID actorUserId;
    public SocialActionEvent(Object source, UUID actorUserId) { super(source); this.actorUserId = actorUserId; }
}
