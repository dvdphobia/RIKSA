package com.riksa.learning.service;

import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.learning.dto.LessonListResponse;
import com.riksa.learning.dto.LessonRoadmapResponse;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.repository.ExerciseAttemptRepository;
import com.riksa.learning.repository.ExerciseRepository;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.social.entity.Category;
import com.riksa.social.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Supplies real, category-ordered learner roadmaps without start/skip state. */
@Service
@RequiredArgsConstructor
public class LessonRoadmapService {
    private final CategoryRepository categoryRepository;
    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final LessonCompletionRepository completionRepository;
    private final LessonProgressionService progressionService;

    @Transactional(readOnly = true)
    public LessonRoadmapResponse getRoadmap(UUID userId, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        List<Lesson> lessons = lessonRepository.findByCategoryIdOrderByPositionAsc(categoryId);
        List<UUID> lessonIds = lessons.stream().map(Lesson::getId).toList();
        Map<UUID, Long> exerciseCounts = lessonIds.isEmpty() ? Map.of()
                : exerciseRepository.countByLessonIds(lessonIds).stream().collect(Collectors.toMap(
                        ExerciseRepository.LessonExerciseCount::getLessonId,
                        ExerciseRepository.LessonExerciseCount::getExerciseCount));
        Set<UUID> completedLessonIds = (userId == null || lessonIds.isEmpty()) ? Set.of()
                : completionRepository.findByUserIdAndLessonIdIn(userId, lessonIds).stream()
                        .map(completion -> completion.getLessonId()).collect(Collectors.toSet());

        LessonRoadmapResponse response = new LessonRoadmapResponse();
        response.setCategoryId(category.getId());
        response.setCategoryName(category.getName());
        response.setLessons(lessons.stream().map(lesson -> toResponse(
                userId, lesson, Math.toIntExact(exerciseCounts.getOrDefault(lesson.getId(), 0L)),
                completedLessonIds.contains(lesson.getId()))).toList());
        return response;
    }

    private LessonListResponse toResponse(UUID userId, Lesson lesson, int exerciseCount, boolean completed) {
        LessonListResponse response = new LessonListResponse();
        response.setId(lesson.getId());
        response.setTitle(lesson.getTitle());
        response.setDescription(lesson.getDescription());
        response.setDifficulty(lesson.getDifficulty());
        response.setBaseXp(lesson.getBaseXp());
        response.setPosition(lesson.getPosition());
        response.setCategoryId(lesson.getCategory() == null ? null : lesson.getCategory().getId());
        response.setCategoryName(lesson.getCategory() == null ? null : lesson.getCategory().getName());
        response.setExerciseCount(exerciseCount);
        response.setCompleted(completed);
        response.setState(progressionService.getState(userId, lesson));
        response.setCreatedAt(lesson.getCreatedAt());
        return response;
    }
}
