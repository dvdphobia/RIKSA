package com.riksa.quest.listener;
import com.riksa.learning.event.LessonCompletedEvent;
import com.riksa.quest.event.ExerciseAnsweredEvent;
import com.riksa.quest.event.SocialActionEvent;
import com.riksa.quest.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
@Component
@RequiredArgsConstructor
public class QuestProgressListener {
    // After commit the finished transaction is still bound to this thread, so
    // a @Transactional write here would join it and never be committed. Like
    // the other gamification listeners, record progress on the async executor,
    // where QuestService opens its own transaction.
    private final QuestService questService;
    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLessonCompleted(LessonCompletedEvent event) { questService.recordLesson(event.getUserId()); }
    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExerciseAnswered(ExerciseAnsweredEvent event) {
        if (event.isCorrect()) questService.recordCorrectAnswer(event.getUserId());
    }
    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSocialAction(SocialActionEvent event) { questService.recordSocialAction(event.getActorUserId()); }
}
