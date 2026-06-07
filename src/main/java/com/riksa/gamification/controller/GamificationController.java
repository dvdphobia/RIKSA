package com.riksa.gamification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.riksa.auth.security.JwtService;
import com.riksa.gamification.dto.GamificationProfileResponse;
import com.riksa.gamification.service.GamificationService;
import java.util.UUID;


import lombok.RequiredArgsConstructor;

@RestController
    @RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
public class GamificationController {
    private final GamificationService gamificationService;
    private final JwtService jwtService;

    @GetMapping("/me")
    public ResponseEntity<GamificationProfileResponse> getProfile(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(gamificationService.getProfile(userId));
    }

    private UUID extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        return jwtService.getUserId(token);
    }

}
