package com.riksa.learning.controller;

import com.riksa.auth.security.JwtService;
import com.riksa.learning.dto.ContinueLearningResponse;
import com.riksa.learning.service.LearningProgressQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class LearningProgressController {
    private final LearningProgressQueryService learningProgressQueryService;
    private final JwtService jwtService;

    @GetMapping("/me/continue")
    public ResponseEntity<ContinueLearningResponse> getContinueLearning(
            @RequestHeader("Authorization") String authHeader) {
        if (!authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        UUID userId = jwtService.getUserId(authHeader.substring(7));
        return ResponseEntity.ok(learningProgressQueryService.getContinueLearning(userId));
    }
}
