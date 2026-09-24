package com.riksa.quest.event;
import com.riksa.common.event.DomainEvent;
import lombok.Getter;
import java.util.UUID;
@Getter
public class ExerciseAnsweredEvent extends DomainEvent {
    private final UUID userId;
    private final UUID exerciseId;
    private final boolean correct;
    public ExerciseAnsweredEvent(Object source, UUID userId, UUID exerciseId, boolean correct) {
        super(source); this.userId = userId; this.exerciseId = exerciseId; this.correct = correct;
    }
}
