package com.riksa.quest.listener;
import com.riksa.learning.event.LessonCompletedEvent;
import com.riksa.quest.event.ExerciseAnsweredEvent;
import com.riksa.quest.event.SocialActionEvent;
import com.riksa.quest.service.QuestService;
import com.riksa.realtime.RealtimeHub;
import com.riksa.realtime.RealtimeTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

@Component
public class QuestProgressListener {
    private final QuestService questService;
    // Told once QuestService's own transaction has committed, so the app's
    // refetch sees the new progress. Null where no hub is wired (unit tests).
    private final RealtimeHub realtimeHub;

    public QuestProgressListener(QuestService questService) {
        this(questService, null);
    }

    @Autowired
    public QuestProgressListener(QuestService questService, RealtimeHub realtimeHub) {
        this.questService = questService;
        this.realtimeHub = realtimeHub;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onLessonCompleted(LessonCompletedEvent event) {
        questService.recordLesson(event.getUserId(), event.getLessonId(), event.getCompletedAt());
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void announceLessonCompletion(LessonCompletedEvent event) {
        announce(event.getUserId());
    }
    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExerciseAnswered(ExerciseAnsweredEvent event) {
        if (!event.isCorrect()) return;
        questService.recordCorrectAnswer(event.getUserId());
        announce(event.getUserId());
    }
    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSocialAction(SocialActionEvent event) {
        questService.recordSocialAction(event.getActorUserId());
        announce(event.getActorUserId());
    }

    private void announce(UUID userId) {
        if (realtimeHub != null) realtimeHub.send(userId, RealtimeTypes.QUESTS_CHANGED, Map.of());
    }
}
