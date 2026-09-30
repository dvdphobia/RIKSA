package com.riksa.admin.controller;

import com.riksa.admin.dto.ExerciseStatsResponse;
import com.riksa.admin.dto.GamificationStatsResponse;
import com.riksa.admin.dto.OverviewStatsResponse;
import com.riksa.admin.dto.SecurityStatsResponse;
import com.riksa.admin.service.AdminStatsService;
import com.riksa.common.constants.AdminEndpoints;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.STATS)
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@RequiredArgsConstructor
public class StatsAdminController {

    private final AdminStatsService adminStatsService;

    @GetMapping(AdminEndpoints.STATS_OVERVIEW)
    public ResponseEntity<OverviewStatsResponse> overview() {
        return ResponseEntity.ok(adminStatsService.getOverview());
    }

    @GetMapping(AdminEndpoints.STATS_EXERCISES)
    public ResponseEntity<List<ExerciseStatsResponse>> exercises(
            @RequestParam(defaultValue = "10") int minAttempts) {
        return ResponseEntity.ok(adminStatsService.getExerciseStats(minAttempts));
    }

    @GetMapping(AdminEndpoints.STATS_GAMIFICATION)
    public ResponseEntity<GamificationStatsResponse> gamification() {
        return ResponseEntity.ok(adminStatsService.getGamificationStats());
    }

    @GetMapping(AdminEndpoints.STATS_SECURITY)
    public ResponseEntity<SecurityStatsResponse> security() {
        return ResponseEntity.ok(adminStatsService.getSecurityStats());
    }
}
