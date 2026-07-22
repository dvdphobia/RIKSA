package com.riksa.learning.service;

import com.riksa.common.exception.ResourceNotFoundException;
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

    @Test
    void comparesNormalizedAnswerAndCreatesAttempt() {
        UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID(), exerciseId = UUID.randomUUID();
        Lesson lesson = new Lesson(); lesson.setId(lessonId);
        Exercise exercise = new Exercise(); exercise.setId(exerciseId); exercise.setLesson(lesson);
        exercise.setAnswerKey("Phnom Penh"); exercise.setPoints(7);
        ExerciseAttemptRequest request = new ExerciseAttemptRequest(); request.setAnswer("  phnom penh ");
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(attemptRepository.findByUserIdAndExerciseId(userId, exerciseId)).thenReturn(Optional.empty());

        var result = new ExerciseAttemptService(exerciseRepository, attemptRepository)
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
        ExerciseAttempt existing = new ExerciseAttempt(); existing.setId(UUID.randomUUID());
        when(attemptRepository.findByUserIdAndExerciseId(userId, exerciseId)).thenReturn(Optional.of(existing));
        assertThat(new ExerciseAttemptService(exerciseRepository, attemptRepository)
                .submitAttempt(userId, lessonId, exerciseId, request).isCorrect()).isFalse();
        verify(attemptRepository).save(existing);

        assertThatThrownBy(() -> new ExerciseAttemptService(exerciseRepository, attemptRepository)
                .submitAttempt(userId, UUID.randomUUID(), exerciseId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
