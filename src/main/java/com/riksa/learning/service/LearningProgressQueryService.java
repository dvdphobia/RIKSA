package com.riksa.learning.service;

import com.riksa.learning.dto.ContinueLearningResponse;
import com.riksa.learning.dto.LearnerLessonProgressResponse;
import com.riksa.learning.dto.LessonProgressState;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.ExerciseAttempt;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.entity.LessonCompletion;
import com.riksa.learning.repository.ExerciseAttemptRepository;
import com.riksa.learning.repository.ExerciseRepository;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Builds a learner-facing progress read model from durable attempts and completions. */
@Service
@RequiredArgsConstructor
public class LearningProgressQueryService {

    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final LessonProgressionService lessonProgressionService;

    @Transactional(readOnly = true)
    public ContinueLearningResponse getContinueLearning(UUID userId) {
        List<Lesson> lessons = lessonRepository.findAll();
        if (lessons.isEmpty()) return new ContinueLearningResponse(true, null);

        List<UUID> lessonIds = lessons.stream().map(Lesson::getId).toList();
        List<Exercise> exercises = exerciseRepository.findByLessonIdIn(lessonIds);
        Map<UUID, List<Exercise>> exercisesByLesson = exercises.stream()
                .collect(Collectors.groupingBy(exercise -> exercise.getLesson().getId()));
        List<UUID> exerciseIds = exercises.stream().map(Exercise::getId).toList();
        List<ExerciseAttempt> attempts = exerciseIds.isEmpty() ? List.of()
                : exerciseAttemptRepository.findByUserIdAndExerciseIdIn(userId, exerciseIds);
        Map<UUID, List<ExerciseAttempt>> attemptsByExercise = attempts.stream()
                .collect(Collectors.groupingBy(ExerciseAttempt::getExerciseId));
        Map<UUID, LessonCompletion> completionsByLesson = lessonCompletionRepository
                .findByUserIdAndLessonIdIn(userId, lessonIds).stream()
                .collect(Collectors.toMap(LessonCompletion::getLessonId, completion -> completion));

        List<ProgressCandidate> candidates = lessons.stream()
                .map(lesson -> candidate(userId, lesson, exercisesByLesson, attemptsByExercise,
                        completionsByLesson))
                .toList();

        Comparator<ProgressCandidate> roadmapOrder = Comparator
                .comparing((ProgressCandidate candidate) -> categoryName(candidate.lesson()), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(candidate -> candidate.lesson().getPosition())
                .thenComparing(candidate -> candidate.lesson().getId());

        ProgressCandidate target = candidates.stream()
                .filter(candidate -> candidate.state() == LessonProgressState.AVAILABLE)
                .filter(candidate -> candidate.response().getAttemptedExerciseCount() > 0)
                .max(Comparator.comparing((ProgressCandidate candidate) -> candidate.response().getLastActivityAt())
                        .thenComparing(roadmapOrder))
                .orElseGet(() -> candidates.stream()
                        .filter(candidate -> candidate.state() == LessonProgressState.AVAILABLE)
                        .min(roadmapOrder)
                        .orElse(null));

        return target == null
                ? new ContinueLearningResponse(true, null)
                : new ContinueLearningResponse(false, target.response());
    }

    private ProgressCandidate candidate(UUID userId, Lesson lesson,
                                        Map<UUID, List<Exercise>> exercisesByLesson,
                                        Map<UUID, List<ExerciseAttempt>> attemptsByExercise,
                                        Map<UUID, LessonCompletion> completionsByLesson) {
        List<Exercise> lessonExercises = exercisesByLesson.getOrDefault(lesson.getId(), List.of());
        Set<UUID> attemptedExerciseIds = new HashSet<>();
        Set<UUID> correctExerciseIds = new HashSet<>();
        OffsetDateTime lastActivityAt = null;

        for (Exercise exercise : lessonExercises) {
            List<ExerciseAttempt> exerciseAttempts = attemptsByExercise.getOrDefault(exercise.getId(), List.of());
            if (!exerciseAttempts.isEmpty()) attemptedExerciseIds.add(exercise.getId());
            if (exerciseAttempts.stream().anyMatch(ExerciseAttempt::isCorrect)) {
                correctExerciseIds.add(exercise.getId());
            }
            for (ExerciseAttempt attempt : exerciseAttempts) {
                if (attempt.getAttemptedAt() != null && (lastActivityAt == null || attempt.getAttemptedAt().isAfter(lastActivityAt))) {
                    lastActivityAt = attempt.getAttemptedAt();
                }
            }
        }

        LessonProgressState state = lessonProgressionService.getState(userId, lesson);
        LessonCompletion completion = completionsByLesson.get(lesson.getId());
        LearnerLessonProgressResponse response = new LearnerLessonProgressResponse();
        response.setLessonId(lesson.getId());
        response.setLessonTitle(lesson.getTitle());
        response.setCategoryId(lesson.getCategory() == null ? null : lesson.getCategory().getId());
        response.setCategoryName(lesson.getCategory() == null ? null : lesson.getCategory().getName());
        response.setRoadmapState(state);
        response.setTotalExerciseCount(lessonExercises.size());
        response.setAttemptedExerciseCount(attemptedExerciseIds.size());
        response.setCorrectExerciseCount(correctExerciseIds.size());
        response.setProgressPercentage(lessonExercises.isEmpty() ? 0D
                : (correctExerciseIds.size() * 100D) / lessonExercises.size());
        response.setCurrentSectionIndex(currentSection(lessonExercises, correctExerciseIds));
        response.setLastActivityAt(lastActivityAt);
        response.setCompleted(state == LessonProgressState.COMPLETED);
        response.setCompletedAt(completion == null ? null : completion.getCompletedAt());
        response.setCanResume(state == LessonProgressState.AVAILABLE && !attemptedExerciseIds.isEmpty());
        response.setDifficulty(lesson.getDifficulty());
        response.setBaseXp(lesson.getBaseXp());
        response.setPosition(lesson.getPosition());
        return new ProgressCandidate(lesson, state, response);
    }

    private int currentSection(Collection<Exercise> exercises, Set<UUID> correctExerciseIds) {
        if (exercises.isEmpty()) return 1;
        return exercises.stream()
                .filter(exercise -> !correctExerciseIds.contains(exercise.getId()))
                .map(Exercise::getSectionIndex)
                .min(Integer::compareTo)
                .orElseGet(() -> exercises.stream().map(Exercise::getSectionIndex)
                        .max(Integer::compareTo).orElse(1));
    }

    private String categoryName(Lesson lesson) {
        return lesson.getCategory() == null || lesson.getCategory().getName() == null
                ? "" : lesson.getCategory().getName();
    }

    private record ProgressCandidate(Lesson lesson, LessonProgressState state, LearnerLessonProgressResponse response) { }
}
