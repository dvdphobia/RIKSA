package com.riksa.admin.controller;

import com.riksa.admin.dto.ReportResolutionRequest;
import com.riksa.admin.service.ReportAdminService;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.common.security.SecurityUtils;
import com.riksa.gamification.dto.CursorPage;
import com.riksa.report.dto.ReportResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.REPORTS)
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@RequiredArgsConstructor
public class ReportAdminController {

    private final ReportAdminService reportAdminService;

    @GetMapping
    public ResponseEntity<CursorPage<ReportResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(reportAdminService.list(status, targetType, cursor, limit));
    }

    @GetMapping(AdminEndpoints.REPORT_BY_ID)
    public ResponseEntity<ReportResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(reportAdminService.get(id));
    }

    @PatchMapping(AdminEndpoints.REPORT_BY_ID)
    public ResponseEntity<ReportResponse> resolve(
            @PathVariable UUID id,
            @RequestBody @Valid ReportResolutionRequest request) {
        return ResponseEntity.ok(reportAdminService.resolve(SecurityUtils.currentUserId(), id, request));
    }
}
