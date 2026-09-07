package com.riksa.learning.service;

import com.riksa.learning.dto.ExerciseAttemptRequest;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.ExerciseAttempt;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.repository.ExerciseAttemptRepository;
import com.riksa.learning.repository.ExerciseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExerciseAttemptServiceTest {

    @Mock ExerciseRepository exerciseRepository;
    @Mock ExerciseAttemptRepository attemptRepository;
    @Mock LessonProgressionService progressionService;

    @Test
    void comparesNormalizedAnswerAndCreatesAttempt() {
        UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID(), exerciseId = UUID.randomUUID();
        Lesson lesson = new Lesson(); lesson.setId(lessonId);
        Exercise exercise = new Exercise(); exercise.setId(exerciseId); exercise.setLesson(lesson);
        exercise.setAnswerKey("Phnom Penh"); exercise.setPoints(7);
        ExerciseAttemptRequest request = new ExerciseAttemptRequest(); request.setAnswer("  phnom penh ");
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(progressionService.requireAccessible(userId, lessonId)).thenReturn(lesson);
        when(attemptRepository.countByUserIdAndExerciseId(userId, exerciseId)).thenReturn(0L);

        var result = new ExerciseAttemptService(exerciseRepository, attemptRepository, progressionService)
                .submitAttempt(userId, lessonId, exerciseId, request);

        assertThat(result.isCorrect()).isTrue();
        ArgumentCaptor<ExerciseAttempt> captor = ArgumentCaptor.forClass(ExerciseAttempt.class);
        verify(attemptRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().isCorrect()).isTrue();
    }

    @Test
    void updatesExistingAttemptAndRejectsWrongLesson() {
        UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID(), exerciseId = UUID.randomUUID();
        Lesson lesson = new Lesson(); lesson.setId(lessonId);
        Exercise exercise = new Exercise(); exercise.setId(exerciseId); exercise.setLesson(lesson); exercise.setAnswerKey("42");
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));

        ExerciseAttemptRequest request = new ExerciseAttemptRequest(); request.setAnswer("41");
        when(progressionService.requireAccessible(userId, lessonId)).thenReturn(lesson);
        when(attemptRepository.countByUserIdAndExerciseId(userId, exerciseId)).thenReturn(1L);
        assertThat(new ExerciseAttemptService(exerciseRepository, attemptRepository, progressionService)
                .submitAttempt(userId, lessonId, exerciseId, request).isCorrect()).isFalse();
        verify(attemptRepository).save(any(ExerciseAttempt.class));

        UUID lockedLessonId = UUID.randomUUID();
        when(progressionService.requireAccessible(userId, lockedLessonId)).thenThrow(new LessonLockedException(lockedLessonId));
        assertThatThrownBy(() -> new ExerciseAttemptService(exerciseRepository, attemptRepository, progressionService)
                .submitAttempt(userId, lockedLessonId, exerciseId, request))
                .isInstanceOf(LessonLockedException.class);
    }

    @Test
    void retry_createsSecondAppendOnlyAttempt() {
        UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID(), exerciseId = UUID.randomUUID();
        Lesson lesson = new Lesson(); lesson.setId(lessonId);
        Exercise exercise = new Exercise(); exercise.setId(exerciseId); exercise.setLesson(lesson); exercise.setAnswerKey("42");
        ExerciseAttemptRequest request = new ExerciseAttemptRequest(); request.setAnswer("42");
        when(progressionService.requireAccessible(userId, lessonId)).thenReturn(lesson);
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(attemptRepository.countByUserIdAndExerciseId(userId, exerciseId)).thenReturn(1L);

        new ExerciseAttemptService(exerciseRepository, attemptRepository, progressionService)
                .submitAttempt(userId, lessonId, exerciseId, request);

        ArgumentCaptor<ExerciseAttempt> captor = ArgumentCaptor.forClass(ExerciseAttempt.class);
        verify(attemptRepository).save(captor.capture());
        assertThat(captor.getValue().getAttemptNumber()).isEqualTo(2);
        assertThat(captor.getValue().getSubmittedAnswer()).isEqualTo("42");
        assertThat(captor.getValue().isCorrect()).isTrue();
    }
}
