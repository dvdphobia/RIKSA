package com.riksa.admin.controller;

import com.riksa.admin.dto.AchievementAdminRequest;
import com.riksa.admin.dto.AchievementAdminResponse;
import com.riksa.admin.dto.RankTierAdminResponse;
import com.riksa.admin.dto.RankTierUpdateRequest;
import com.riksa.admin.dto.StreakMilestoneAdminResponse;
import com.riksa.admin.dto.StreakMilestoneRequest;
import com.riksa.admin.dto.XpAuditRow;
import com.riksa.admin.dto.XpBoostAdminResponse;
import com.riksa.admin.dto.XpBoostRequest;
import com.riksa.admin.service.GamificationAdminService;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.ADMIN_GAMIFICATION)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class GamificationAdminController {

    private final GamificationAdminService gamificationAdminService;

    @GetMapping(AdminEndpoints.ADMIN_ACHIEVEMENTS)
    public ResponseEntity<List<AchievementAdminResponse>> listAchievements() {
        return ResponseEntity.ok(gamificationAdminService.listAchievements());
    }

    @PostMapping(AdminEndpoints.ADMIN_ACHIEVEMENTS)
    public ResponseEntity<List<AchievementAdminResponse>> createAchievement(
            @RequestBody @Valid AchievementAdminRequest request) {
        return ResponseEntity.ok(gamificationAdminService.createAchievement(
                SecurityUtils.currentUserId(), request));
    }

    @PutMapping(AdminEndpoints.ADMIN_ACHIEVEMENTS + "/{id}")
    public ResponseEntity<List<AchievementAdminResponse>> updateAchievement(
            @PathVariable UUID id, @RequestBody @Valid AchievementAdminRequest request) {
        return ResponseEntity.ok(gamificationAdminService.updateAchievement(
                SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping(AdminEndpoints.ADMIN_ACHIEVEMENTS + "/{id}")
    public ResponseEntity<List<AchievementAdminResponse>> deleteAchievement(@PathVariable UUID id) {
        return ResponseEntity.ok(gamificationAdminService.deleteAchievement(SecurityUtils.currentUserId(), id));
    }

    @GetMapping(AdminEndpoints.ADMIN_RANK_TIERS)
    public ResponseEntity<List<RankTierAdminResponse>> listRankTiers() {
        return ResponseEntity.ok(gamificationAdminService.listRankTiers());
    }

    @PutMapping(AdminEndpoints.ADMIN_RANK_TIERS + "/{id}")
    public ResponseEntity<List<RankTierAdminResponse>> updateRankTier(
            @PathVariable UUID id, @RequestBody @Valid RankTierUpdateRequest request) {
        return ResponseEntity.ok(gamificationAdminService.updateRankTier(
                SecurityUtils.currentUserId(), id, request));
    }

    @GetMapping(AdminEndpoints.ADMIN_STREAK_MILESTONES)
    public ResponseEntity<List<StreakMilestoneAdminResponse>> listStreakMilestones() {
        return ResponseEntity.ok(gamificationAdminService.listStreakMilestones());
    }

    @PostMapping(AdminEndpoints.ADMIN_STREAK_MILESTONES)
    public ResponseEntity<List<StreakMilestoneAdminResponse>> createStreakMilestone(
            @RequestBody @Valid StreakMilestoneRequest request) {
        return ResponseEntity.ok(gamificationAdminService.createStreakMilestone(
                SecurityUtils.currentUserId(), request));
    }

    @PutMapping(AdminEndpoints.ADMIN_STREAK_MILESTONES + "/{id}")
    public ResponseEntity<List<StreakMilestoneAdminResponse>> updateStreakMilestone(
            @PathVariable UUID id, @RequestBody @Valid StreakMilestoneRequest request) {
        return ResponseEntity.ok(gamificationAdminService.updateStreakMilestone(
                SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping(AdminEndpoints.ADMIN_STREAK_MILESTONES + "/{id}")
    public ResponseEntity<List<StreakMilestoneAdminResponse>> deleteStreakMilestone(@PathVariable UUID id) {
        return ResponseEntity.ok(gamificationAdminService.deleteStreakMilestone(SecurityUtils.currentUserId(), id));
    }

    @GetMapping(AdminEndpoints.ADMIN_XP_BOOSTS)
    public ResponseEntity<List<XpBoostAdminResponse>> listXpBoosts() {
        return ResponseEntity.ok(gamificationAdminService.listXpBoosts());
    }

    @PostMapping(AdminEndpoints.ADMIN_XP_BOOSTS)
    public ResponseEntity<List<XpBoostAdminResponse>> createXpBoost(@RequestBody @Valid XpBoostRequest request) {
        return ResponseEntity.ok(gamificationAdminService.createXpBoost(SecurityUtils.currentUserId(), request));
    }

    @DeleteMapping(AdminEndpoints.ADMIN_XP_BOOSTS + "/{id}")
    public ResponseEntity<List<XpBoostAdminResponse>> cancelXpBoost(@PathVariable UUID id) {
        return ResponseEntity.ok(gamificationAdminService.cancelXpBoost(SecurityUtils.currentUserId(), id));
    }

    @GetMapping(AdminEndpoints.ADMIN_XP_AUDIT)
    public ResponseEntity<List<XpAuditRow>> xpAudit(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "0") long minXp,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(gamificationAdminService.xpAudit(hours, minXp, limit));
    }
}
