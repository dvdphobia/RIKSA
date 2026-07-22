package com.riksa.learning.controller;

import com.riksa.auth.security.JwtService;
import com.riksa.common.enums.DifficultyLevel;
import com.riksa.learning.dto.AnswerResult;
import com.riksa.learning.dto.CompletionRequest;
import com.riksa.learning.dto.CompletionResponse;
import com.riksa.learning.dto.ExerciseAttemptRequest;
import com.riksa.learning.dto.LessonDetailResponse;
import com.riksa.learning.dto.LessonListResponse;
import com.riksa.learning.service.ExerciseAttemptService;
import com.riksa.learning.service.LessonCompletionService;
import com.riksa.learning.service.LessonService;
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
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final ExerciseAttemptService exerciseAttemptService;
    private final LessonCompletionService lessonCompletionService;
    private final JwtService jwtService;

    @GetMapping
    public ResponseEntity<Page<LessonListResponse>> listLessons(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(lessonService.listLessons(userId, categoryId, difficulty, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LessonDetailResponse> getLesson(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {
        extractUserId(authHeader); // validate token
        return ResponseEntity.ok(lessonService.getLesson(id));
    }

    @PostMapping("/{id}/exercises/{exerciseId}/attempt")
    public ResponseEntity<AnswerResult> submitAttempt(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id,
            @PathVariable UUID exerciseId,
            @Valid @RequestBody ExerciseAttemptRequest request) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(exerciseAttemptService.submitAttempt(userId, id, exerciseId, request));
    }

    @PostMapping("/{id}/complete")
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
