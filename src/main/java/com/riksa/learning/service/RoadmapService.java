package com.riksa.learning.service;

import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.learning.dto.*;
import com.riksa.learning.entity.*;
import com.riksa.learning.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadmapService {

    private final RoadmapUnitRepository roadmapUnitRepository;
    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final UserLessonProgressRepository userLessonProgressRepository;
    private final RoadmapUnitSkipRepository roadmapUnitSkipRepository;
    private final LessonRewardPolicy lessonRewardPolicy;

    @Transactional(readOnly = true)
    public RoadmapResponse getRoadmap(UUID userId, UUID categoryId) {
        List<RoadmapUnit> units = roadmapUnitRepository.findByCategoryIdOrderByPositionAsc(categoryId);
        if (units.isEmpty()) {
            throw new ResourceNotFoundException("Roadmap not found for category: " + categoryId);
        }

        List<UUID> unitIds = units.stream().map(RoadmapUnit::getId).toList();
        List<Lesson> lessons = lessonRepository.findRoadmapLessons(unitIds);
        List<UUID> lessonIds = lessons.stream().map(Lesson::getId).toList();
        List<Exercise> exercises = lessonIds.isEmpty() ? List.of() : exerciseRepository.findByLessonIdIn(lessonIds);
        Map<UUID, List<Exercise>> exercisesByLesson = exercises.stream()
                .collect(Collectors.groupingBy(exercise -> exercise.getLesson().getId()));

        List<LessonCompletion> completions = (userId == null || lessonIds.isEmpty()) ? List.of()
                : lessonCompletionRepository.findByUserIdAndLessonIdIn(userId, lessonIds);
        Map<UUID, LessonCompletion> completionByLesson = completions.stream()
                .collect(Collectors.toMap(LessonCompletion::getLessonId, Function.identity()));
        Map<UUID, UserLessonProgress> progressByLesson = (userId == null || lessonIds.isEmpty()) ? Map.of()
                : userLessonProgressRepository.findByUserIdAndLessonIdIn(userId, lessonIds).stream()
                .collect(Collectors.toMap(UserLessonProgress::getLessonId, Function.identity()));
        Map<UUID, Long> correctByLesson = (userId == null || lessonIds.isEmpty()) ? Map.of()
                : exerciseAttemptRepository.countCorrectByUserIdAndLessonIds(userId, lessonIds).stream()
                .collect(Collectors.toMap(
                        ExerciseAttemptRepository.LessonCorrectCount::getLessonId,
                        ExerciseAttemptRepository.LessonCorrectCount::getCorrectCount));
        Set<UUID> skippedUnitIds = (userId == null || unitIds.isEmpty()) ? Set.of()
                : roadmapUnitSkipRepository
                .findByUserIdAndRoadmapUnitIdIn(userId, unitIds).stream()
                .map(RoadmapUnitSkip::getRoadmapUnitId)
                .collect(Collectors.toSet());
        Map<UUID, List<Lesson>> lessonsByUnit = lessons.stream()
                .collect(Collectors.groupingBy(lesson -> lesson.getRoadmapUnit().getId(), LinkedHashMap::new, Collectors.toList()));

        List<RoadmapUnitResponse> unitResponses = new ArrayList<>();
        UUID currentLessonId = null;
        boolean pathOpen = true;
        for (RoadmapUnit unit : units) {
            List<Lesson> unitLessons = lessonsByUnit.getOrDefault(unit.getId(), List.of());
            long completedInUnit = unitLessons.stream().filter(l -> completionByLesson.containsKey(l.getId())).count();
            boolean complete = !unitLessons.isEmpty() && completedInUnit == unitLessons.size();
            boolean skipped = skippedUnitIds.contains(unit.getId()) && !complete;
            String unitStatus;
            if (complete) unitStatus = "COMPLETED";
            else if (skipped) unitStatus = "SKIPPED";
            else if (pathOpen) unitStatus = "CURRENT";
            else unitStatus = "LOCKED";

            boolean lessonOpen = "CURRENT".equals(unitStatus);
            List<RoadmapLessonResponse> lessonResponses = new ArrayList<>();
            for (Lesson lesson : unitLessons) {
                List<Exercise> lessonExercises = exercisesByLesson.getOrDefault(lesson.getId(), List.of());
                boolean completed = completionByLesson.containsKey(lesson.getId());
                boolean started = progressByLesson.containsKey(lesson.getId());
                String lessonStatus;
                boolean current = false;
                if (completed) {
                    lessonStatus = "COMPLETED";
                } else if ("SKIPPED".equals(unitStatus)) {
                    lessonStatus = "SKIPPED";
                } else if (lessonOpen) {
                    lessonStatus = started ? "IN_PROGRESS" : "AVAILABLE";
                    current = true;
                    lessonOpen = false;
                    if (currentLessonId == null) currentLessonId = lesson.getId();
                } else {
                    lessonStatus = "LOCKED";
                }

                long correct = correctByLesson.getOrDefault(lesson.getId(), 0L);
                double progress = completed ? 1.0
                        : lessonExercises.isEmpty() ? 0.0
                        : Math.min(1.0, (double) correct / lessonExercises.size());
                lessonResponses.add(new RoadmapLessonResponse(
                        lesson.getId(), lesson.getTitle(), lesson.getDescription(), lesson.getDifficulty(),
                        lesson.getBaseXp(), lessonRewardPolicy.potentialXp(lesson, lessonExercises),
                        lesson.getEstimatedMinutes(), lessonExercises.size(), progress, lessonStatus, current));
            }

            unitResponses.add(new RoadmapUnitResponse(
                    unit.getId(), unit.getTitle(), unit.getDescription(), unit.getPosition(), unitStatus,
                    Math.toIntExact(completedInUnit), unitLessons.size(), "CURRENT".equals(unitStatus), lessonResponses));
            if (!complete && !skipped) pathOpen = false;
        }

        int completedLessons = completionByLesson.size();
        double totalProgress = lessons.isEmpty() ? 0.0 : (double) completedLessons / lessons.size();
        Set<java.time.LocalDate> activeDates = new TreeSet<>();
        progressByLesson.values().stream()
                .map(UserLessonProgress::getUpdatedAt).filter(Objects::nonNull)
                .map(OffsetDateTime::toLocalDate).forEach(activeDates::add);
        completions.stream().map(LessonCompletion::getCompletedAt).filter(Objects::nonNull)
                .map(OffsetDateTime::toLocalDate).forEach(activeDates::add);

        RoadmapUnit first = units.getFirst();
        return new RoadmapResponse(categoryId, first.getCategory().getName(), completedLessons, lessons.size(),
                totalProgress, currentLessonId, List.copyOf(activeDates), unitResponses);
    }

    @Transactional
    public LessonProgressResponse startLesson(UUID userId, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));
        String responseStatus = "IN_PROGRESS";
        if (lesson.getRoadmapUnit() != null) {
            RoadmapLessonResponse roadmapLesson = getRoadmap(userId, lesson.getCategory().getId()).units().stream()
                    .flatMap(unit -> unit.lessons().stream())
                    .filter(item -> item.id().equals(lessonId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Roadmap lesson not found: " + lessonId));
            if ("LOCKED".equals(roadmapLesson.status()) || "SKIPPED".equals(roadmapLesson.status())) {
                throw new IllegalStateException("Lesson is not currently available: " + lessonId);
            }
            if ("COMPLETED".equals(roadmapLesson.status())) responseStatus = "COMPLETED";
        }
        UserLessonProgress progress = userLessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(UserLessonProgress::new);
        if (progress.getId() == null) {
            progress.setUserId(userId);
            progress.setLessonId(lessonId);
        }
        progress.setLastAccessedAt(OffsetDateTime.now());
        UserLessonProgress saved = userLessonProgressRepository.save(progress);
        return new LessonProgressResponse(lessonId, responseStatus, saved.getLastAccessedAt());
    }

    @Transactional
    public void skipUnit(UUID userId, UUID unitId) {
        RoadmapUnit unit = roadmapUnitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Roadmap unit not found: " + unitId));
        RoadmapUnitResponse response = getRoadmap(userId, unit.getCategory().getId()).units().stream()
                .filter(item -> item.id().equals(unitId)).findFirst().orElseThrow();
        if ("LOCKED".equals(response.status())) {
            throw new IllegalStateException("Roadmap unit is locked: " + unitId);
        }
        if ("COMPLETED".equals(response.status()) || "SKIPPED".equals(response.status())) return;
        RoadmapUnitSkip skip = new RoadmapUnitSkip();
        skip.setUserId(userId);
        skip.setRoadmapUnitId(unitId);
        roadmapUnitSkipRepository.save(skip);
    }

    @Transactional
    public void resumeUnit(UUID userId, UUID unitId) {
        if (!roadmapUnitRepository.existsById(unitId)) {
            throw new ResourceNotFoundException("Roadmap unit not found: " + unitId);
        }
        roadmapUnitSkipRepository.deleteByUserIdAndRoadmapUnitId(userId, unitId);
    }
}
