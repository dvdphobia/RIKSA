package com.riksa.admin.controller;

import com.riksa.admin.dto.RoadmapUnitRequest;
import com.riksa.admin.dto.RoadmapUnitReorderRequest;
import com.riksa.admin.dto.RoadmapUnitResponse;
import com.riksa.admin.service.RoadmapUnitAdminService;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.ROADMAP_UNITS)
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@RequiredArgsConstructor
public class RoadmapUnitAdminController {

    private final RoadmapUnitAdminService roadmapUnitAdminService;

    @GetMapping(AdminEndpoints.ROOT)
    public ResponseEntity<List<RoadmapUnitResponse>> list(
            @RequestParam(required = false) UUID categoryId) {
        return ResponseEntity.ok(roadmapUnitAdminService.list(categoryId));
    }

    @PostMapping(AdminEndpoints.ROOT)
    public ResponseEntity<RoadmapUnitResponse> create(@RequestBody @Valid RoadmapUnitRequest request) {
        return ResponseEntity.ok(roadmapUnitAdminService.create(SecurityUtils.currentUserId(), request));
    }

    @PutMapping(AdminEndpoints.ROADMAP_UNIT_BY_ID)
    public ResponseEntity<RoadmapUnitResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid RoadmapUnitRequest request) {
        return ResponseEntity.ok(roadmapUnitAdminService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping(AdminEndpoints.ROADMAP_UNIT_BY_ID)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        roadmapUnitAdminService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(AdminEndpoints.ROADMAP_UNIT_REORDER)
    public ResponseEntity<List<RoadmapUnitResponse>> reorder(
            @RequestBody @Valid RoadmapUnitReorderRequest request) {
        return ResponseEntity.ok(roadmapUnitAdminService.reorder(SecurityUtils.currentUserId(), request));
    }
}
