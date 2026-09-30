package com.riksa.admin.controller;

import com.riksa.admin.dto.AdminUserResponse;
import com.riksa.admin.dto.AchievementGrantRequest;
import com.riksa.admin.dto.RoleUpdateRequest;
import com.riksa.admin.dto.StreakAdjustmentRequest;
import com.riksa.admin.dto.SuspensionRequest;
import com.riksa.admin.dto.UserOverviewResponse;
import com.riksa.admin.dto.XpAdjustmentRequest;
import com.riksa.gamification.dto.XpTransactionResponse;
import com.riksa.admin.service.UserAdminService;
import com.riksa.auth.entity.Role;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.common.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.USERS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Validated
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean suspended,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userAdminService.list(query, role, suspended, page, size));
    }

    @GetMapping(AdminEndpoints.USER_OVERVIEW)
    public ResponseEntity<UserOverviewResponse> overview(@PathVariable UUID id) {
        return ResponseEntity.ok(userAdminService.getOverview(id));
    }

    @GetMapping(AdminEndpoints.USER_XP)
    public ResponseEntity<Page<XpTransactionResponse>> xpHistory(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userAdminService.xpHistory(id, page, size));
    }

    @PostMapping(AdminEndpoints.USER_XP)
    public ResponseEntity<UserOverviewResponse> adjustXp(
            @PathVariable UUID id,
            @RequestBody @Valid XpAdjustmentRequest request) {
        return ResponseEntity.ok(userAdminService.adjustXp(SecurityUtils.currentUserId(), id, request));
    }

    @PostMapping(AdminEndpoints.USER_STREAK)
    public ResponseEntity<UserOverviewResponse> adjustStreak(
            @PathVariable UUID id,
            @RequestBody @Valid StreakAdjustmentRequest request) {
        return ResponseEntity.ok(userAdminService.adjustStreak(SecurityUtils.currentUserId(), id, request));
    }

    @PostMapping(AdminEndpoints.USER_ACHIEVEMENTS)
    public ResponseEntity<UserOverviewResponse> grantAchievement(
            @PathVariable UUID id,
            @PathVariable UUID achievementId,
            @RequestBody @Valid AchievementGrantRequest request) {
        return ResponseEntity.ok(userAdminService.grantAchievement(
                SecurityUtils.currentUserId(), id, achievementId, request));
    }

    @DeleteMapping(AdminEndpoints.USER_ACHIEVEMENTS)
    public ResponseEntity<UserOverviewResponse> revokeAchievement(
            @PathVariable UUID id,
            @PathVariable UUID achievementId,
            @RequestParam @NotBlank @Size(max = 500) String reason) {
        return ResponseEntity.ok(userAdminService.revokeAchievement(
                SecurityUtils.currentUserId(), id, achievementId, reason));
    }

    @DeleteMapping(AdminEndpoints.USER_LOCK)
    public ResponseEntity<UserOverviewResponse> unlock(@PathVariable UUID id) {
        return ResponseEntity.ok(userAdminService.unlock(SecurityUtils.currentUserId(), id));
    }

    @PutMapping(AdminEndpoints.USER_LEADERBOARD_EXCLUSION)
    public ResponseEntity<UserOverviewResponse> setLeaderboardExclusion(
            @PathVariable UUID id,
            @RequestParam boolean excluded,
            @RequestParam @NotBlank @Size(max = 500) String reason) {
        return ResponseEntity.ok(userAdminService.setLeaderboardExclusion(
                SecurityUtils.currentUserId(), id, excluded, reason));
    }

    @PostMapping(AdminEndpoints.USER_ROLE)
    public ResponseEntity<AdminUserResponse> updateRole(
            @PathVariable UUID id,
            @RequestBody @Valid RoleUpdateRequest request) {
        return ResponseEntity.ok(userAdminService.updateRole(SecurityUtils.currentUserId(), id, request.getRole()));
    }

    @PostMapping(AdminEndpoints.USER_SUSPENSION)
    public ResponseEntity<AdminUserResponse> suspend(
            @PathVariable UUID id,
            @RequestBody @Valid SuspensionRequest request) {
        return ResponseEntity.ok(userAdminService.suspend(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping(AdminEndpoints.USER_SUSPENSION)
    public ResponseEntity<AdminUserResponse> unsuspend(@PathVariable UUID id) {
        return ResponseEntity.ok(userAdminService.unsuspend(SecurityUtils.currentUserId(), id));
    }
}
