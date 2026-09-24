package com.riksa.learning.controller;

import com.riksa.learning.dto.AnswerResult;
import com.riksa.learning.dto.ExerciseAttemptRequest;
import com.riksa.learning.service.GuestExerciseCheckService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GuestExerciseCheckController {
    private final GuestExerciseCheckService guestExerciseCheckService;

    @Value("${rate-limit.trust-forward-headers:false}")
    private boolean trustForwardHeaders;

    @PostMapping("/api/v1/learning/lessons/{lessonId}/exercises/{exerciseId}/check")
    public ResponseEntity<AnswerResult> check(
            @PathVariable UUID lessonId,
            @PathVariable UUID exerciseId,
            @Valid @RequestBody ExerciseAttemptRequest request,
            HttpServletRequest servletRequest) {
        return ResponseEntity.ok(guestExerciseCheckService.check(
                clientIp(servletRequest), lessonId, exerciseId, request));
    }

    private String clientIp(HttpServletRequest request) {
        if (trustForwardHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
