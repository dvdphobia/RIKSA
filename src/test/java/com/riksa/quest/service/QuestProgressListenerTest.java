package com.riksa.quest.service;

import com.riksa.learning.event.LessonCompletedEvent;
import com.riksa.quest.event.ExerciseAnsweredEvent;
import com.riksa.quest.event.SocialActionEvent;
import com.riksa.quest.listener.QuestProgressListener;
import com.riksa.quest.service.QuestService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;
import java.util.Arrays;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class QuestProgressListenerTest {
    private static final UUID USER_ID = UUID.fromString("3eb0153c-7e8c-4c93-85f2-169093a328df");
    private static final UUID SOURCE_ID = UUID.fromString("807e046d-4bbf-4c21-9b65-a7eeb4dafb2f");

    @Test
    void progressEventsMapToTheirQuestTypesAndIgnoreWrongAnswers() {
        QuestService service = mock(QuestService.class);
        QuestProgressListener listener = new QuestProgressListener(service);
        OffsetDateTime completedAt = OffsetDateTime.parse("2026-11-10T10:00:00+07:00");
        var lessonEvent = new LessonCompletedEvent(this, USER_ID, SOURCE_ID, 10, completedAt);

        listener.onLessonCompleted(lessonEvent);
        listener.announceLessonCompletion(lessonEvent);
        listener.onExerciseAnswered(new ExerciseAnsweredEvent(this, USER_ID, SOURCE_ID, true));
        listener.onExerciseAnswered(new ExerciseAnsweredEvent(this, USER_ID, SOURCE_ID, false));
        listener.onSocialAction(new SocialActionEvent(this, USER_ID));

        verify(service).recordLesson(USER_ID, SOURCE_ID, completedAt);
        verify(service).recordCorrectAnswer(USER_ID);
        verify(service).recordSocialAction(USER_ID);
        verifyNoMoreInteractions(service);
    }

    @Test
    void lessonProgressRunsBeforeCommitAndNotificationsAfterCommit() throws Exception {
        var lessonProgress = QuestProgressListener.class.getDeclaredMethod("onLessonCompleted", LessonCompletedEvent.class);
        assertThat(lessonProgress.getAnnotation(TransactionalEventListener.class).phase())
                .isEqualTo(TransactionPhase.BEFORE_COMMIT);
        assertThat(lessonProgress.getAnnotation(Async.class)).isNull();

        var afterCommitMethods = Arrays.stream(QuestProgressListener.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("announceLessonCompletion")
                        || method.getName().equals("onExerciseAnswered")
                        || method.getName().equals("onSocialAction"))
                .toList();
        assertThat(afterCommitMethods).hasSize(3);
        assertThat(afterCommitMethods)
                .allSatisfy(method -> assertThat(method.getAnnotation(TransactionalEventListener.class))
                        .isNotNull()
                        .extracting(TransactionalEventListener::phase)
                        .isEqualTo(TransactionPhase.AFTER_COMMIT));
        assertThat(afterCommitMethods)
                .allSatisfy(method -> assertThat(method.getAnnotation(Async.class))
                        .isNotNull()
                        .extracting(Async::value)
                        .isEqualTo("gamificationTaskExecutor"));
    }
}
