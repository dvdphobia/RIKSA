package com.riksa.admin.service;

import com.riksa.admin.dto.*;
import com.riksa.common.enums.DifficultyLevel;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.repository.ExerciseRepository;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.social.entity.Category;
import com.riksa.social.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonAdminService {

    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;
    private final CategoryRepository categoryRepository;
    private final LessonMarkdownParser markdownParser;

    @Transactional
    public LessonResponse create(LessonCreateRequest req) {
        Category category = resolveCategory(req.getCategoryId());

        Lesson lesson = new Lesson();
        lesson.setTitle(req.getTitle());
        lesson.setDescription(req.getDescription());
        lesson.setDifficulty(req.getDifficulty());
        lesson.setBaseXp(req.getBaseXp());
        lesson.setCategory(category);
        lesson.setPosition(resolvePosition(category, req.getPosition(), null));
        lesson.setReflectionPrompt(req.getReflectionPrompt());
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
        boolean categoryChanged = req.getCategoryId() != null
                && (lesson.getCategory() == null || !lesson.getCategory().getId().equals(req.getCategoryId()));
        if (req.getCategoryId() != null) lesson.setCategory(resolveCategory(req.getCategoryId()));
        if (req.getPosition() != null || categoryChanged) {
            lesson.setPosition(resolvePosition(lesson.getCategory(), req.getPosition(), lesson.getId()));
        }
        if (req.getReflectionPrompt() != null) lesson.setReflectionPrompt(req.getReflectionPrompt());

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
    public LessonResponse get(UUID id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + id));
        return toResponse(lesson, exerciseRepository.findByLessonId(id));
    }

    @Transactional(readOnly = true)
    public Page<LessonResponse> list(int page, int size, String query, DifficultyLevel difficulty, UUID categoryId) {
        int safeSize = Math.max(1, Math.min(size, 100));
        String q = query == null ? "" : query.trim();
        Pageable pageable = PageRequest.of(page, safeSize);
        Page<Lesson> lessons;
        if (difficulty != null && categoryId != null) {
            lessons = lessonRepository.searchByTitleAndDifficultyAndCategoryId(q, difficulty, categoryId, pageable);
        } else if (difficulty != null) {
            lessons = lessonRepository.searchByTitleAndDifficulty(q, difficulty, pageable);
        } else if (categoryId != null) {
            lessons = lessonRepository.searchByTitleAndCategoryId(q, categoryId, pageable);
        } else {
            lessons = lessonRepository.searchByTitle(q, pageable);
        }

        List<UUID> lessonIds = lessons.getContent().stream().map(Lesson::getId).toList();
        Map<UUID, List<Exercise>> exercisesByLesson = lessonIds.isEmpty()
                ? Map.of()
                : exerciseRepository.findByLessonIdIn(lessonIds).stream()
                        .collect(Collectors.groupingBy(e -> e.getLesson().getId()));

        return lessons.map(lesson ->
                toResponse(lesson, exercisesByLesson.getOrDefault(lesson.getId(), List.of())));
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

    @Transactional
    public LessonResponse uploadMarkdown(MultipartFile file) {
        return create(parseMarkdownFile(file));
    }

    @Transactional
    public BulkLessonResult uploadMarkdownBulk(List<MultipartFile> files) {
        int success = 0;
        int failure = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String label = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file " + i;
            try {
                create(parseMarkdownFile(file));
                success++;
            } catch (Exception e) {
                failure++;
                errors.add(label + ": " + e.getMessage());
                log.warn("Markdown upload failed for {}: {}", label, e.getMessage());
            }
        }
        return new BulkLessonResult(success, failure, errors);
    }

    private LessonCreateRequest parseMarkdownFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.toLowerCase().endsWith(".md") && !filename.toLowerCase().endsWith(".markdown")) {
            throw new IllegalArgumentException("Only .md files are supported, got: " + filename);
        }
        try {
            return markdownParser.parse(new String(file.getBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read uploaded file: " + e.getMessage());
        }
    }

    // --- helpers ---

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        // Learner-made communities must never become part of the curriculum.
        if (!category.isOfficial()) {
            throw new IllegalArgumentException("Lessons can only use official categories");
        }
        return category;
    }

    private List<Exercise> buildExercises(List<ExerciseRequest> requests, Lesson lesson) {
        return requests.stream().map(er -> {
            validateExercise(er);
            Exercise e = new Exercise();
            e.setLesson(lesson);
            e.setQuestion(er.getQuestion());
            e.setAnswerKey(er.getAnswerKey());
            e.setDifficulty(er.getDifficulty());
            e.setPoints(er.getPoints());
            e.setSectionIndex(er.getSectionIndex());
            e.setType(er.getType());
            e.setOptions(normalizeOptions(er.getOptions()));
            e.setHint(er.getHint());
            e.setExplanation(er.getExplanation());
            return e;
        }).toList();
    }

    private int resolvePosition(Category category, Integer requestedPosition, UUID excludeId) {
        if (requestedPosition != null) {
            if (lessonRepository.existsAtPosition(category == null ? null : category.getId(), requestedPosition, excludeId)) {
                throw new IllegalArgumentException("A lesson already uses position " + requestedPosition
                        + " in this category");
            }
            return requestedPosition;
        }
        return category == null
                ? lessonRepository.findFirstByCategoryIsNullOrderByPositionDesc().map(l -> l.getPosition() + 1).orElse(1)
                : lessonRepository.findFirstByCategoryIdOrderByPositionDesc(category.getId())
                        .map(l -> l.getPosition() + 1).orElse(1);
    }

    private void validateExercise(ExerciseRequest exercise) {
        List<String> options = normalizeOptions(exercise.getOptions());
        if (exercise.getType() == com.riksa.learning.entity.ExerciseType.MULTIPLE_CHOICE) {
            if (options.size() < 2 || !options.contains(exercise.getAnswerKey().trim())) {
                throw new IllegalArgumentException("MULTIPLE_CHOICE exercises need at least two options including answerKey");
            }
        }
        if (exercise.getType() == com.riksa.learning.entity.ExerciseType.TRUE_FALSE) {
            if (!isTrueFalse(exercise.getAnswerKey()) || (!options.isEmpty() && !options.equals(List.of("True", "False")))) {
                throw new IllegalArgumentException("TRUE_FALSE answerKey must be True or False and options must be True/False");
            }
        }
    }

    private boolean isTrueFalse(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private List<String> normalizeOptions(List<String> options) {
        if (options == null) return List.of();
        return options.stream().filter(java.util.Objects::nonNull).map(String::trim)
                .filter(option -> !option.isEmpty()).toList();
    }

    private LessonResponse toResponse(Lesson lesson, List<Exercise> exercises) {
        LessonResponse resp = new LessonResponse();
        resp.setId(lesson.getId());
        resp.setTitle(lesson.getTitle());
        resp.setDescription(lesson.getDescription());
        resp.setDifficulty(lesson.getDifficulty().name());
        resp.setBaseXp(lesson.getBaseXp());
        resp.setPosition(lesson.getPosition());
        resp.setCategoryId(lesson.getCategory() != null ? lesson.getCategory().getId() : null);
        resp.setReflectionPrompt(lesson.getReflectionPrompt());
        resp.setExercises(exercises.stream().map(e -> new ExerciseResponse(e.getId(), e.getQuestion(), e.getAnswerKey(),
                e.getDifficulty().name(), e.getPoints(), e.getSectionIndex(), e.getType(), e.getOptions(), e.getHint(),
                e.getExplanation())).toList());
        resp.setCreatedAt(lesson.getCreatedAt());
        resp.setUpdatedAt(lesson.getUpdatedAt());
        return resp;
    }
}
