package com.riksa.admin.controller;

import com.riksa.admin.dto.LeaderboardBoardResponse;
import com.riksa.admin.dto.LeaderboardMetaResponse;
import com.riksa.admin.dto.LeaderboardRefreshResponse;
import com.riksa.admin.service.LeaderboardAdminService;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.common.enums.ScopeType;
import com.riksa.common.security.SecurityUtils;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.ADMIN_LEADERBOARD)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Validated
public class LeaderboardAdminController {

    private final LeaderboardAdminService leaderboardAdminService;

    @GetMapping(AdminEndpoints.ADMIN_LEADERBOARD_META)
    public ResponseEntity<LeaderboardMetaResponse> meta() {
        return ResponseEntity.ok(leaderboardAdminService.meta());
    }

    @GetMapping(AdminEndpoints.ROOT)
    public ResponseEntity<LeaderboardBoardResponse> board(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "COUNTRY") ScopeType scope,
            @RequestParam(defaultValue = "CAMBODIA") String scopeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(leaderboardAdminService.board(date, scope, scopeId, page, size));
    }

    @PostMapping(AdminEndpoints.ADMIN_LEADERBOARD_REFRESH)
    public ResponseEntity<LeaderboardRefreshResponse> refresh() {
        return ResponseEntity.ok(leaderboardAdminService.refresh(SecurityUtils.currentUserId()));
    }
}
