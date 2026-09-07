package com.riksa.learning.service;

import com.riksa.learning.dto.LessonProgressState;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.ExerciseAttempt;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.entity.LessonCompletion;
import com.riksa.learning.repository.ExerciseAttemptRepository;
import com.riksa.learning.repository.ExerciseRepository;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningProgressQueryServiceTest {

    @Mock LessonRepository lessonRepository;
    @Mock ExerciseRepository exerciseRepository;
    @Mock ExerciseAttemptRepository attemptRepository;
    @Mock LessonCompletionRepository completionRepository;
    @Mock LessonProgressionService progressionService;

    @Test
    void brandNewLearnerGetsFirstAvailableLessonWithZeroProgress() {
        UUID userId = UUID.randomUUID();
        Lesson first = lesson("Algebra", 1), locked = lesson("Equations", 2);
        Exercise exercise = exercise(first, 1);
        arrange(List.of(first, locked), List.of(exercise), List.of(), List.of());
        when(progressionService.getState(userId, first)).thenReturn(LessonProgressState.AVAILABLE);
        when(progressionService.getState(userId, locked)).thenReturn(LessonProgressState.LOCKED);

        var result = service().getContinueLearning(userId);

        assertThat(result.isAllCompleted()).isFalse();
        assertThat(result.getLesson().getLessonId()).isEqualTo(first.getId());
        assertThat(result.getLesson().getProgressPercentage()).isZero();
        assertThat(result.getLesson().getCurrentSectionIndex()).isEqualTo(1);
        assertThat(result.getLesson().isCanResume()).isFalse();
    }

    @Test
    void partialLessonIsResumableAndCountsRepeatedAttemptsByUniqueExercise() {
        UUID userId = UUID.randomUUID();
        Lesson lesson = lesson("Algebra", 1);
        Exercise first = exercise(lesson, 1), second = exercise(lesson, 2);
        OffsetDateTime latest = OffsetDateTime.now();
        ExerciseAttempt wrong = attempt(first, false, latest.minusMinutes(2));
        ExerciseAttempt correctRetry = attempt(first, true, latest.minusMinutes(1));
        ExerciseAttempt secondWrong = attempt(second, false, latest);
        arrange(List.of(lesson), List.of(first, second), List.of(wrong, correctRetry, secondWrong), List.of());
        when(progressionService.getState(userId, lesson)).thenReturn(LessonProgressState.AVAILABLE);

        var progress = service().getContinueLearning(userId).getLesson();

        assertThat(progress.getAttemptedExerciseCount()).isEqualTo(2);
        assertThat(progress.getCorrectExerciseCount()).isEqualTo(1);
        assertThat(progress.getProgressPercentage()).isEqualTo(50D);
        assertThat(progress.getCurrentSectionIndex()).isEqualTo(2);
        assertThat(progress.getLastActivityAt()).isEqualTo(latest);
        assertThat(progress.isCanResume()).isTrue();
    }

    @Test
    void completedLessonCausesNextAvailableLessonToBeRecommendedAndNeverReturnsLocked() {
        UUID userId = UUID.randomUUID();
        Lesson completed = lesson("Algebra", 1), next = lesson("Equations", 2), locked = lesson("Graphs", 3);
        LessonCompletion completion = new LessonCompletion();
        completion.setLessonId(completed.getId());
        completion.setCompletedAt(OffsetDateTime.now());
        arrange(List.of(completed, next, locked), List.of(), List.of(), List.of(completion));
        when(progressionService.getState(userId, completed)).thenReturn(LessonProgressState.COMPLETED);
        when(progressionService.getState(userId, next)).thenReturn(LessonProgressState.AVAILABLE);
        when(progressionService.getState(userId, locked)).thenReturn(LessonProgressState.LOCKED);

        var result = service().getContinueLearning(userId);

        assertThat(result.isAllCompleted()).isFalse();
        assertThat(result.getLesson().getLessonId()).isEqualTo(next.getId());
        assertThat(result.getLesson().getRoadmapState()).isEqualTo(LessonProgressState.AVAILABLE);
    }

    @Test
    void allCompletedReturnsExplicitTerminalResponse() {
        UUID userId = UUID.randomUUID();
        Lesson first = lesson("Algebra", 1), second = lesson("Equations", 2);
        arrange(List.of(first, second), List.of(), List.of(), List.of());
        when(progressionService.getState(userId, first)).thenReturn(LessonProgressState.COMPLETED);
        when(progressionService.getState(userId, second)).thenReturn(LessonProgressState.COMPLETED);

        var result = service().getContinueLearning(userId);

        assertThat(result.isAllCompleted()).isTrue();
        assertThat(result.getLesson()).isNull();
    }

    private void arrange(List<Lesson> lessons, List<Exercise> exercises,
                         List<ExerciseAttempt> attempts, List<LessonCompletion> completions) {
        when(lessonRepository.findAll()).thenReturn(lessons);
        when(exerciseRepository.findByLessonIdIn(anyList())).thenReturn(exercises);
        if (!exercises.isEmpty()) {
            when(attemptRepository.findByUserIdAndExerciseIdIn(org.mockito.ArgumentMatchers.any(), anyList()))
                    .thenReturn(attempts);
        }
        when(completionRepository.findByUserIdAndLessonIdIn(org.mockito.ArgumentMatchers.any(), anyList()))
                .thenReturn(completions);
    }

    private LearningProgressQueryService service() {
        return new LearningProgressQueryService(lessonRepository, exerciseRepository, attemptRepository,
                completionRepository, progressionService);
    }

    private Lesson lesson(String title, int position) {
        Lesson lesson = new Lesson();
        lesson.setId(UUID.randomUUID());
        lesson.setTitle(title);
        lesson.setPosition(position);
        return lesson;
    }

    private Exercise exercise(Lesson lesson, int sectionIndex) {
        Exercise exercise = new Exercise();
        exercise.setId(UUID.randomUUID());
        exercise.setLesson(lesson);
        exercise.setSectionIndex(sectionIndex);
        return exercise;
    }

    private ExerciseAttempt attempt(Exercise exercise, boolean correct, OffsetDateTime attemptedAt) {
        ExerciseAttempt attempt = new ExerciseAttempt();
        attempt.setExerciseId(exercise.getId());
        attempt.setCorrect(correct);
        attempt.setAttemptedAt(attemptedAt);
        return attempt;
    }
}
