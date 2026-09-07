package com.riksa.learning.service;

import com.riksa.common.enums.DifficultyLevel;
import com.riksa.learning.dto.LessonProgressState;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.ExerciseAttempt;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.entity.LessonCompletion;
import com.riksa.learning.repository.ExerciseRepository;
import com.riksa.learning.repository.ExerciseAttemptRepository;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock LessonRepository lessonRepository;
    @Mock ExerciseRepository exerciseRepository;
    @Mock ExerciseAttemptRepository exerciseAttemptRepository;
    @Mock LessonCompletionRepository completionRepository;
    @Mock LessonProgressionService progressionService;

    @Test
    void listLessonsPopulatesExerciseCountAndCompletionInBatches() {
        UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson(); lesson.setId(lessonId); lesson.setTitle("Algebra");
        when(lessonRepository.findByDifficultyOrderByPositionAsc(
                eq(DifficultyLevel.EASY), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(lesson)));
        ExerciseRepository.LessonExerciseCount count = mock(ExerciseRepository.LessonExerciseCount.class);
        when(count.getLessonId()).thenReturn(lessonId); when(count.getExerciseCount()).thenReturn(3L);
        when(exerciseRepository.countByLessonIds(List.of(lessonId))).thenReturn(List.of(count));
        LessonCompletion completion = new LessonCompletion(); completion.setLessonId(lessonId);
        when(completionRepository.findByUserIdAndLessonIdIn(userId, List.of(lessonId)))
                .thenReturn(List.of(completion));

        when(progressionService.getState(userId, lesson)).thenReturn(LessonProgressState.COMPLETED);
        var result = new LessonService(lessonRepository, exerciseRepository, exerciseAttemptRepository, completionRepository, progressionService)
                .listLessons(userId, null, DifficultyLevel.EASY, 0, 20);

        assertThat(result.getContent().getFirst().getExerciseCount()).isEqualTo(3);
        assertThat(result.getContent().getFirst().isCompleted()).isTrue();
        verify(exerciseRepository, times(1)).countByLessonIds(anyList());
    }

    @Test
    void getLessonMapsExercisesAndRejectsLockedLesson() {
        UUID userId = UUID.randomUUID(), lessonId = UUID.randomUUID();
        Lesson lesson = new Lesson(); lesson.setId(lessonId); lesson.setTitle("Physics");
        Exercise exercise = new Exercise();
        exercise.setId(UUID.randomUUID());
        exercise.setQuestion("Q");
        exercise.setSectionIndex(2);
        when(progressionService.requireAccessible(userId, lessonId)).thenReturn(lesson);
        when(progressionService.getState(userId, lesson)).thenReturn(LessonProgressState.AVAILABLE);
        when(exerciseRepository.findByLessonId(lessonId)).thenReturn(List.of(exercise));
        ExerciseAttempt correctAttempt = new ExerciseAttempt();
        correctAttempt.setExerciseId(exercise.getId());
        correctAttempt.setCorrect(true);
        when(exerciseAttemptRepository.findByUserIdAndExerciseIdIn(eq(userId), anyList()))
                .thenReturn(List.of(correctAttempt));
        LessonService service = new LessonService(lessonRepository, exerciseRepository, exerciseAttemptRepository, completionRepository, progressionService);
        var response = service.getLesson(userId, lessonId).getExercises();
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getSectionIndex()).isEqualTo(2);
        assertThat(response.getFirst().isCompleted()).isTrue();

        UUID missing = UUID.randomUUID();
        when(progressionService.requireAccessible(userId, missing)).thenThrow(new LessonLockedException(missing));
        assertThatThrownBy(() -> service.getLesson(userId, missing)).isInstanceOf(LessonLockedException.class);
    }
}
