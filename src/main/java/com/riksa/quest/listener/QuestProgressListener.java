package com.riksa.quest.listener;
import com.riksa.learning.event.LessonCompletedEvent;
import com.riksa.quest.event.ExerciseAnsweredEvent;
import com.riksa.quest.event.SocialActionEvent;
import com.riksa.quest.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
@Component
@RequiredArgsConstructor
public class QuestProgressListener {
    private final QuestService questService;
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLessonCompleted(LessonCompletedEvent event) { questService.recordLesson(event.getUserId()); }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExerciseAnswered(ExerciseAnsweredEvent event) {
        if (event.isCorrect()) questService.recordCorrectAnswer(event.getUserId());
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSocialAction(SocialActionEvent event) { questService.recordSocialAction(event.getActorUserId()); }
}
