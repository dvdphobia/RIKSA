package com.riksa.learning.service;

import com.riksa.common.event.EventPublisher;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private final LessonRewardPolicy lessonRewardPolicy = new LessonRewardPolicy();

    private LessonCompletionService service() {
        return new LessonCompletionService(lessonRepository, exerciseRepository, attemptRepository,
                completionRepository, reflectionValidator, eventPublisher, lessonRewardPolicy, progressionService);
    }

    @Test
    void completesLessonCalculatesXpAndPublishesEvent() {
        UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson(); lesson.setId(lessonId); lesson.setBaseXp(10);
        Exercise first = exercise(lesson, 5), second = exercise(lesson, 7);
        ExerciseAttempt a1 = correct(first), a2 = correct(second);
        CompletionRequest request = new CompletionRequest(); request.setReflectionText("valid reflection text");
        when(progressionService.requireAccessible(userId, lessonId)).thenReturn(lesson);
        when(exerciseRepository.findByLessonId(lessonId)).thenReturn(List.of(first, second));
        when(attemptRepository.findByUserIdAndExerciseIdIn(eq(userId), anyList())).thenReturn(List.of(a1, a2));
        when(reflectionValidator.validate(request.getReflectionText())).thenReturn(2);
        when(completionRepository.save(any())).thenAnswer(inv -> {
            LessonCompletion c = inv.getArgument(0); c.setId(UUID.randomUUID()); return c;
        });

        var response = service().complete(userId, lessonId, request);

        assertThat(response.getXpEarned()).isEqualTo(22);
        assertThat(response.getCompletionId()).isNotNull();
        assertThat(response.isPassed()).isTrue();
        verify(eventPublisher).publish(any(LessonCompletedEvent.class));
    }

    @Test
    void preventsDuplicateAndRequiresEveryCorrectAttempt() {
        UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson(); lesson.setId(lessonId);
        CompletionRequest request = new CompletionRequest(); request.setReflectionText("valid reflection text");
        when(progressionService.requireAccessible(userId, lessonId)).thenReturn(lesson);
        when(completionRepository.existsByUserIdAndLessonId(userId, lessonId)).thenReturn(true);
        assertThatThrownBy(() -> service().complete(userId, lessonId, request))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("already completed");

        reset(completionRepository);
        Exercise exercise = exercise(lesson, 5);
        when(progressionService.requireAccessible(userId, lessonId)).thenReturn(lesson);
        when(exerciseRepository.findByLessonId(lessonId)).thenReturn(List.of(exercise));
        when(attemptRepository.findByUserIdAndExerciseIdIn(eq(userId), anyList())).thenReturn(List.of());
        assertThatThrownBy(() -> service().complete(userId, lessonId, request))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("0/1");
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void rejectsCompletionOfLockedLesson() {
        UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID();
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

    private ExerciseAttempt correct(Exercise exercise) {
        ExerciseAttempt attempt = new ExerciseAttempt(); attempt.setExerciseId(exercise.getId());
        attempt.setCorrect(true); return attempt;
    }
}
