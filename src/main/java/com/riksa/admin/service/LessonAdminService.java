package com.riksa.admin.service;

import com.riksa.admin.dto.*;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.repository.ExerciseRepository;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.social.entity.Category;
import com.riksa.social.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonAdminService {

    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public LessonResponse create(LessonCreateRequest req) {
        Category category = resolveCategory(req.getCategoryId());

        Lesson lesson = new Lesson();
        lesson.setTitle(req.getTitle());
        lesson.setDescription(req.getDescription());
        lesson.setDifficulty(req.getDifficulty());
        lesson.setBaseXp(req.getBaseXp());
        lesson.setCategory(category);
        lesson = lessonRepository.save(lesson);

        List<Exercise> exercises = buildExercises(req.getExercises(), lesson);
        exerciseRepository.saveAll(exercises);

        return toResponse(lesson, exercises);
    }

    @Transactional
    public LessonResponse update(UUID id, LessonUpdateRequest req) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + id));

        if (req.getTitle() != null) lesson.setTitle(req.getTitle());
        if (req.getDescription() != null) lesson.setDescription(req.getDescription());
        if (req.getDifficulty() != null) lesson.setDifficulty(req.getDifficulty());
        if (req.getBaseXp() != null) lesson.setBaseXp(req.getBaseXp());
        if (req.getCategoryId() != null) lesson.setCategory(resolveCategory(req.getCategoryId()));

        // Replace exercises if provided
        List<Exercise> exercises;
        if (req.getExercises() != null && !req.getExercises().isEmpty()) {
            exerciseRepository.deleteByLessonId(lesson.getId());
            exerciseRepository.flush();
            exercises = buildExercises(req.getExercises(), lesson);
            exerciseRepository.saveAll(exercises);
        } else {
            exercises = exerciseRepository.findByLessonId(lesson.getId());
        }

        lesson = lessonRepository.save(lesson);
        return toResponse(lesson, exercises);
    }

    @Transactional
    public void delete(UUID id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + id));
        // exercises are cascaded via DB ON DELETE CASCADE
        lessonRepository.delete(lesson);
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> list(int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        var pageable = org.springframework.data.domain.PageRequest.of(page, safeSize);
        return lessonRepository.findAll(pageable).stream()
                .map(lesson -> {
                    List<Exercise> exercises = exerciseRepository.findByLessonId(lesson.getId());
                    return toResponse(lesson, exercises);
                })
                .toList();
    }

    @Transactional
    public BulkLessonResult bulkCreate(List<LessonCreateRequest> requests) {
        int success = 0;
        int failure = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            try {
                create(requests.get(i));
                success++;
            } catch (Exception e) {
                failure++;
                errors.add("Item " + i + ": " + e.getMessage());
                log.warn("Bulk lesson create failed at index {}: {}", i, e.getMessage());
            }
        }
        return new BulkLessonResult(success, failure, errors);
    }

    // --- helpers ---

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
    }

    private List<Exercise> buildExercises(List<ExerciseRequest> requests, Lesson lesson) {
        return requests.stream().map(er -> {
            Exercise e = new Exercise();
            e.setLesson(lesson);
            e.setQuestion(er.getQuestion());
            e.setAnswerKey(er.getAnswerKey());
            e.setDifficulty(er.getDifficulty());
            e.setPoints(er.getPoints());
            return e;
        }).toList();
    }

    private LessonResponse toResponse(Lesson lesson, List<Exercise> exercises) {
        LessonResponse resp = new LessonResponse();
        resp.setId(lesson.getId());
        resp.setTitle(lesson.getTitle());
        resp.setDescription(lesson.getDescription());
        resp.setDifficulty(lesson.getDifficulty().name());
        resp.setBaseXp(lesson.getBaseXp());
        resp.setCategoryId(lesson.getCategory() != null ? lesson.getCategory().getId() : null);
        resp.setExercises(exercises.stream().map(e ->
                new ExerciseResponse(e.getId(), e.getQuestion(), e.getDifficulty().name(), e.getPoints())
        ).toList());
        resp.setCreatedAt(lesson.getCreatedAt());
        resp.setUpdatedAt(lesson.getUpdatedAt());
        return resp;
    }
}