package com.riksa.learning.service;

import com.riksa.common.enums.DifficultyLevel;
import com.riksa.learning.dto.ExerciseResponse;
import com.riksa.learning.dto.LessonDetailResponse;
import com.riksa.learning.dto.LessonListResponse;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.ExerciseAttempt;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.repository.ExerciseRepository;
import com.riksa.learning.repository.ExerciseAttemptRepository;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final LessonProgressionService lessonProgressionService;

    /**
     * List lessons with optional filtering by category and difficulty.
     * Results are paged (page/size) in curriculum position order.
     */
    @Transactional(readOnly = true)
    public Page<LessonListResponse> listLessons(UUID userId, UUID categoryId,
                                                DifficultyLevel difficulty, int page, int size) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);

        PageRequest pageable = PageRequest.of(page, size);
        Page<Lesson> lessons;
        if (categoryId == null && difficulty == null) {
            lessons = lessonRepository.findAllByOrderByPositionAsc(pageable);
        } else if (categoryId != null && difficulty == null) {
            lessons = lessonRepository.findByCategoryIdOrderByPositionAsc(categoryId, pageable);
        } else if (categoryId == null) {
            lessons = lessonRepository.findByDifficultyOrderByPositionAsc(difficulty, pageable);
        } else {
            lessons = lessonRepository.findByCategoryIdAndDifficultyOrderByPositionAsc(
                    categoryId, difficulty, pageable);
        }

        List<UUID> lessonIds = lessons.stream().map(Lesson::getId).toList();
        if (lessonIds.isEmpty()) return lessons.map(lesson -> toListResponse(userId, lesson, 0, false));

        Map<UUID, Long> exerciseCounts = exerciseRepository.countByLessonIds(lessonIds).stream()
                .collect(Collectors.toMap(
                        ExerciseRepository.LessonExerciseCount::getLessonId,
                        ExerciseRepository.LessonExerciseCount::getExerciseCount));
        Set<UUID> completedLessonIds = (userId == null || lessonIds.isEmpty()) ? Set.of()
                : lessonCompletionRepository
                .findByUserIdAndLessonIdIn(userId, lessonIds).stream()
                .map(completion -> completion.getLessonId())
                .collect(Collectors.toSet());

        return lessons.map(lesson -> toListResponse(
                userId,
                lesson,
                Math.toIntExact(exerciseCounts.getOrDefault(lesson.getId(), 0L)),
                completedLessonIds.contains(lesson.getId())));
    }

    /**
     * Get a single lesson with its exercises.
     */
    @Transactional(readOnly = true)
    public LessonDetailResponse getLesson(UUID userId, UUID lessonId) {
        Lesson lesson = lessonProgressionService.requireAccessible(userId, lessonId);

        List<Exercise> exercises = exerciseRepository.findByLessonId(lessonId);
        return toDetailResponse(userId, lesson, exercises);
    }

    private LessonListResponse toListResponse(UUID userId, Lesson lesson, int exerciseCount, boolean completed) {
        LessonListResponse r = new LessonListResponse();
        r.setId(lesson.getId());
        r.setTitle(lesson.getTitle());
        r.setDescription(lesson.getDescription());
        r.setDifficulty(lesson.getDifficulty());
        r.setBaseXp(lesson.getBaseXp());
        r.setPosition(lesson.getPosition());
        r.setCategoryId(lesson.getCategory() != null ? lesson.getCategory().getId() : null);
        r.setCategoryName(lesson.getCategory() != null ? lesson.getCategory().getName() : null);
        r.setExerciseCount(exerciseCount);
        r.setCompleted(completed);
        r.setState(lessonProgressionService.getState(userId, lesson));
        r.setCreatedAt(lesson.getCreatedAt());
        return r;
    }

    private LessonDetailResponse toDetailResponse(UUID userId, Lesson lesson, List<Exercise> exercises) {
        LessonDetailResponse r = new LessonDetailResponse();
        r.setId(lesson.getId());
        r.setTitle(lesson.getTitle());
        r.setDescription(lesson.getDescription());
        r.setDifficulty(lesson.getDifficulty());
        r.setBaseXp(lesson.getBaseXp());
        r.setPosition(lesson.getPosition());
        r.setCategoryId(lesson.getCategory() != null ? lesson.getCategory().getId() : null);
        r.setCategoryName(lesson.getCategory() != null ? lesson.getCategory().getName() : null);
        r.setReflectionPrompt(lesson.getReflectionPrompt());
        r.setState(lessonProgressionService.getState(userId, lesson));
        r.setCreatedAt(lesson.getCreatedAt());
        r.setUpdatedAt(lesson.getUpdatedAt());
        Set<UUID> completedExerciseIds = (userId == null || exercises.isEmpty()) ? Set.of()
                : exerciseAttemptRepository.findByUserIdAndExerciseIdIn(
                                userId, exercises.stream().map(Exercise::getId).toList())
                        .stream()
                        .filter(ExerciseAttempt::isCorrect)
                        .map(ExerciseAttempt::getExerciseId)
                        .collect(Collectors.toSet());
        r.setExercises(exercises.stream()
                .map(exercise -> toExerciseResponse(exercise, completedExerciseIds.contains(exercise.getId())))
                .toList());
        return r;
    }

    private ExerciseResponse toExerciseResponse(Exercise e, boolean completed) {
        ExerciseResponse r = new ExerciseResponse();
        r.setId(e.getId());
        r.setQuestion(e.getQuestion());
        r.setDifficulty(e.getDifficulty());
        r.setPoints(e.getPoints());
        r.setSectionIndex(e.getSectionIndex());
        r.setType(e.getType());
        r.setOptions(e.getOptions());
        r.setHint(e.getHint());
        r.setExplanation(completed ? e.getExplanation() : null);
        r.setCompleted(completed);
        return r;
    }
}
