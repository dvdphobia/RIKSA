package com.riksa.gamification.controller;

import com.riksa.auth.security.JwtService;
import com.riksa.gamification.dto.AchievementResponse;
import com.riksa.gamification.dto.CursorPage;
import com.riksa.gamification.dto.GamificationProfileResponse;
import com.riksa.gamification.dto.LeaderboardEntryResponse;
import com.riksa.gamification.dto.RankHistoryResponse;
import com.riksa.gamification.dto.StreakResponse;
import com.riksa.gamification.dto.XpTransactionResponse;
import com.riksa.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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

    @GetMapping("/xp")
    public ResponseEntity<CursorPage<XpTransactionResponse>> getXpHistory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(gamificationService.getXpHistory(userId, cursor, limit));
    }

    @GetMapping("/achievements")
    public ResponseEntity<List<AchievementResponse>> getAchievements(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(gamificationService.getAchievements(userId));
    }

    @GetMapping("/rank-history")
    public ResponseEntity<CursorPage<RankHistoryResponse>> getRankHistory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(gamificationService.getRankHistory(userId, cursor, limit));
    }

    @GetMapping("/streak")
    public ResponseEntity<StreakResponse> getStreak(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(gamificationService.getStreak(userId));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<CursorPage<LeaderboardEntryResponse>> getLeaderboard(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(gamificationService.getLeaderboard(cursor, limit));
    }

    private UUID extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        return jwtService.getUserId(token);
    }
}