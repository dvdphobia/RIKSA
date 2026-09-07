package com.riksa.learning.controller;

import com.riksa.auth.security.JwtService;
import com.riksa.common.constants.LearningEndpoints;
import com.riksa.common.enums.DifficultyLevel;
import com.riksa.learning.dto.AnswerResult;
import com.riksa.learning.dto.CompletionRequest;
import com.riksa.learning.dto.CompletionResponse;
import com.riksa.learning.dto.ExerciseAttemptRequest;
import com.riksa.learning.dto.LessonDetailResponse;
import com.riksa.learning.dto.LessonListResponse;
import com.riksa.learning.dto.LessonProgressResponse;
import com.riksa.learning.dto.RoadmapResponse;
import com.riksa.learning.service.ExerciseAttemptService;
import com.riksa.learning.service.LessonCompletionService;
import com.riksa.learning.service.LessonService;
import com.riksa.learning.service.RoadmapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(LearningEndpoints.BASE)
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final ExerciseAttemptService exerciseAttemptService;
    private final LessonCompletionService lessonCompletionService;
    private final RoadmapService roadmapService;
    private final JwtService jwtService;

    @GetMapping(LearningEndpoints.ROOT)
    public ResponseEntity<Page<LessonListResponse>> listLessons(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(lessonService.listLessons(userId, categoryId, difficulty, page, size));
    }

    @GetMapping(LearningEndpoints.LESSON_BY_ID)
    public ResponseEntity<LessonDetailResponse> getLesson(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(lessonService.getLesson(userId, id));
    }

    @GetMapping(LearningEndpoints.ROADMAP)
    public ResponseEntity<RoadmapResponse> getRoadmap(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam UUID categoryId) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(roadmapService.getRoadmap(userId, categoryId));
    }

    @PostMapping(LearningEndpoints.START)
    public ResponseEntity<LessonProgressResponse> startLesson(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(roadmapService.startLesson(userId, id));
    }

    @PostMapping(LearningEndpoints.SKIP_ROADMAP_UNIT)
    public ResponseEntity<Void> skipRoadmapUnit(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID unitId) {
        roadmapService.skipUnit(extractUserId(authHeader), unitId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(LearningEndpoints.RESUME_ROADMAP_UNIT)
    public ResponseEntity<Void> resumeRoadmapUnit(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID unitId) {
        roadmapService.resumeUnit(extractUserId(authHeader), unitId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(LearningEndpoints.EXERCISE_ATTEMPT)
    public ResponseEntity<AnswerResult> submitAttempt(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id,
            @PathVariable UUID exerciseId,
            @Valid @RequestBody ExerciseAttemptRequest request) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(exerciseAttemptService.submitAttempt(userId, id, exerciseId, request));
    }

    @PostMapping(LearningEndpoints.COMPLETE)
    public ResponseEntity<CompletionResponse> complete(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id,
            @Valid @RequestBody CompletionRequest request) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(lessonCompletionService.complete(userId, id, request));
    }

    private UUID extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        return jwtService.getUserId(token);
    }
}
