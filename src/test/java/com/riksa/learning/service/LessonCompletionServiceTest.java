package com.riksa.learning.service;

import com.riksa.common.event.EventPublisher;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.service.XpLedgerService;
import com.riksa.learning.dto.CompletionRequest;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.ExerciseAttempt;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.entity.LessonCompletion;
import com.riksa.learning.event.LessonCompletedEvent;
import com.riksa.learning.repository.ExerciseAttemptRepository;
import com.riksa.learning.repository.ExerciseRepository;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonCompletionServiceTest {

    @Mock LessonRepository lessonRepository;
    @Mock ExerciseRepository exerciseRepository;
    @Mock ExerciseAttemptRepository attemptRepository;
    @Mock LessonCompletionRepository completionRepository;
    @Mock ReflectionValidator reflectionValidator;
    @Mock EventPublisher eventPublisher;
    @Mock LessonProgressionService progressionService;
    @Mock UserGamificationRepository gamificationRepository;
    @Mock XpLedgerService xpLedgerService;
    private final LessonRewardPolicy lessonRewardPolicy = new LessonRewardPolicy();

    private LessonCompletionService service() {
        return new LessonCompletionService(lessonRepository, exerciseRepository, attemptRepository,
                completionRepository, reflectionValidator, eventPublisher, lessonRewardPolicy, progressionService,
                gamificationRepository, xpLedgerService);
    }

    @Test
    void completesLessonCalculatesXpAndPublishesEvent() {
        UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID();
        lockUser(userId);
        Lesson lesson = new Lesson(); lesson.setId(lessonId); lesson.setBaseXp(10);
        Exercise first = exercise(lesson, 5), second = exercise(lesson, 7);
        ExerciseAttempt a1 = correct(first), a2 = correct(second);
        CompletionRequest request = new CompletionRequest(); request.setReflectionText("valid reflection text");
        when(progressionService.requireAccessible(userId, lessonId)).thenReturn(lesson);
        when(completionRepository.findByUserIdAndLessonId(userId, lessonId)).thenReturn(Optional.empty());
        when(exerciseRepository.findByLessonId(lessonId)).thenReturn(List.of(first, second));
        when(attemptRepository.findByUserIdAndExerciseIdIn(eq(userId), anyList())).thenReturn(List.of(a1, a2));
        when(reflectionValidator.validate(request.getReflectionText())).thenReturn(2);
        when(completionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            LessonCompletion c = inv.getArgument(0); c.setId(UUID.randomUUID()); return c;
        });
        when(completionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(xpLedgerService.awardXpAndGetResult(eq(userId), eq(22), eq("LESSON_COMPLETION"), eq(lessonId)))
                .thenReturn(new XpLedgerService.XpAwardResult(13, 113));

        var response = service().complete(userId, lessonId, request);

        assertThat(response.getXpEarned()).isEqualTo(13);
        assertThat(response.getCompletionId()).isNotNull();
        assertThat(response.isPassed()).isTrue();
        verify(xpLedgerService).awardXpAndGetResult(userId, 22, "LESSON_COMPLETION", lessonId);
        ArgumentCaptor<LessonCompletedEvent> event = ArgumentCaptor.forClass(LessonCompletedEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().getXpEarned()).isEqualTo(13);
        assertThat(event.getValue().getCompletedAt()).isEqualTo(response.getCompletedAt());
    }

    @Test
    void retryReturnsExistingCompletionAndDoesNotRecreateIt() {
        UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID();
        lockUser(userId);
        Lesson lesson = new Lesson(); lesson.setId(lessonId);
        LessonCompletion completion = new LessonCompletion();
        completion.setId(UUID.randomUUID());
        completion.setUserId(userId);
        completion.setLessonId(lessonId);
        completion.setXpEarned(14);
        completion.setCompletedAt(OffsetDateTime.parse("2026-11-10T10:00:00+07:00"));
        CompletionRequest request = new CompletionRequest(); request.setReflectionText("valid reflection text");
        when(progressionService.requireAccessible(userId, lessonId)).thenReturn(lesson);
        when(completionRepository.findByUserIdAndLessonId(userId, lessonId)).thenReturn(Optional.of(completion));
        when(xpLedgerService.awardXpAndGetResult(eq(userId), eq(14), eq("LESSON_COMPLETION"), eq(lessonId)))
                .thenReturn(new XpLedgerService.XpAwardResult(14, 114));

        var response = service().complete(userId, lessonId, request);

        assertThat(response.getCompletionId()).isEqualTo(completion.getId());
        assertThat(response.getXpEarned()).isEqualTo(14);
        assertThat(response.getCompletedAt()).isEqualTo(completion.getCompletedAt());
        verify(completionRepository, never()).saveAndFlush(any());
        verify(completionRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(exerciseRepository, attemptRepository, reflectionValidator);
    }

    @Test
    void requiresEveryCorrectAttemptBeforePersistingACompletion() {
        UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID();
        lockUser(userId);
        Lesson lesson = new Lesson(); lesson.setId(lessonId);
        CompletionRequest request = new CompletionRequest(); request.setReflectionText("valid reflection text");
        when(progressionService.requireAccessible(userId, lessonId)).thenReturn(lesson);
        when(completionRepository.findByUserIdAndLessonId(userId, lessonId)).thenReturn(Optional.empty());
        Exercise exercise = exercise(lesson, 5);
        when(exerciseRepository.findByLessonId(lessonId)).thenReturn(List.of(exercise));
        when(attemptRepository.findByUserIdAndExerciseIdIn(eq(userId), anyList())).thenReturn(List.of());
        assertThatThrownBy(() -> service().complete(userId, lessonId, request))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("0/1");
        verify(completionRepository, never()).saveAndFlush(any());
        verifyNoInteractions(xpLedgerService);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void rejectsCompletionOfLockedLesson() {
        UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID();
        lockUser(userId);
        CompletionRequest request = new CompletionRequest();
        when(progressionService.requireAccessible(userId, lessonId))
                .thenThrow(new LessonLockedException(lessonId));

        assertThatThrownBy(() -> service().complete(userId, lessonId, request))
                .isInstanceOf(LessonLockedException.class);

        verifyNoInteractions(exerciseRepository, attemptRepository, completionRepository, eventPublisher);
    }

    private Exercise exercise(Lesson lesson, int points) {
        Exercise exercise = new Exercise(); exercise.setId(UUID.randomUUID());
        exercise.setLesson(lesson); exercise.setPoints(points); return exercise;
    }

    private void lockUser(UUID userId) {
        UserGamification gamification = new UserGamification();
        gamification.setUserId(userId);
        when(gamificationRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(gamification));
    }

    private ExerciseAttempt correct(Exercise exercise) {
        ExerciseAttempt attempt = new ExerciseAttempt(); attempt.setExerciseId(exercise.getId());
        attempt.setCorrect(true); return attempt;
    }
}
