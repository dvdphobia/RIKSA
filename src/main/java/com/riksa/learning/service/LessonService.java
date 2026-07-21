package com.riksa.learning.service;

import com.riksa.common.enums.DifficultyLevel;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.learning.dto.ExerciseResponse;
import com.riksa.learning.dto.LessonDetailResponse;
import com.riksa.learning.dto.LessonListResponse;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.repository.ExerciseRepository;
import com.riksa.learning.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;

    /**
     * List lessons with optional filtering by category and difficulty.
     * Results are paged (page/size) ordered by createdAt descending.
     */
    @Transactional(readOnly = true)
    public Page<LessonListResponse> listLessons(UUID categoryId, DifficultyLevel difficulty, int page, int size) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);

        Page<Lesson> lessons = lessonRepository.findByFilters(
                categoryId, difficulty, PageRequest.of(page, size));

        return lessons.map(this::toListResponse);
    }

    /**
     * Get a single lesson with its exercises.
     */
    @Transactional(readOnly = true)
    public LessonDetailResponse getLesson(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));

        List<Exercise> exercises = exerciseRepository.findByLessonId(lessonId);
        return toDetailResponse(lesson, exercises);
    }

    private LessonListResponse toListResponse(Lesson lesson) {
        LessonListResponse r = new LessonListResponse();
        r.setId(lesson.getId());
        r.setTitle(lesson.getTitle());
        r.setDescription(lesson.getDescription());
        r.setDifficulty(lesson.getDifficulty());
        r.setBaseXp(lesson.getBaseXp());
        r.setCategoryId(lesson.getCategory() != null ? lesson.getCategory().getId() : null);
        r.setCategoryName(lesson.getCategory() != null ? lesson.getCategory().getName() : null);
        r.setCreatedAt(lesson.getCreatedAt());
        return r;
    }

    private LessonDetailResponse toDetailResponse(Lesson lesson, List<Exercise> exercises) {
        LessonDetailResponse r = new LessonDetailResponse();
        r.setId(lesson.getId());
        r.setTitle(lesson.getTitle());
        r.setDescription(lesson.getDescription());
        r.setDifficulty(lesson.getDifficulty());
        r.setBaseXp(lesson.getBaseXp());
        r.setCategoryId(lesson.getCategory() != null ? lesson.getCategory().getId() : null);
        r.setCategoryName(lesson.getCategory() != null ? lesson.getCategory().getName() : null);
        r.setReflectionPrompt(lesson.getReflectionPrompt());
        r.setCreatedAt(lesson.getCreatedAt());
        r.setUpdatedAt(lesson.getUpdatedAt());
        r.setExercises(exercises.stream().map(this::toExerciseResponse).toList());
        return r;
    }

    private ExerciseResponse toExerciseResponse(Exercise e) {
        ExerciseResponse r = new ExerciseResponse();
        r.setId(e.getId());
        r.setQuestion(e.getQuestion());
        r.setDifficulty(e.getDifficulty());
        r.setPoints(e.getPoints());
        return r;
    }
}