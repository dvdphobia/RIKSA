package com.riksa.admin.controller;

import com.riksa.admin.dto.AdminLessonResource;
import com.riksa.admin.dto.AdminLessonResourceStatusRequest;
import com.riksa.admin.service.LessonResourceAdminService;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.ADMIN_RESOURCES)
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@RequiredArgsConstructor
public class LessonResourceAdminController {

    private final LessonResourceAdminService resourceAdminService;

    @GetMapping
    public ResponseEntity<Page<AdminLessonResource>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID lessonId,
            @RequestParam(required = false) Boolean hidden,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(resourceAdminService.list(lessonId, hidden, query, page, size));
    }

    @PatchMapping(AdminEndpoints.ADMIN_RESOURCE_STATUS)
    public ResponseEntity<AdminLessonResource> updateStatus(
            @PathVariable UUID id,
            @RequestBody @Valid AdminLessonResourceStatusRequest request) {
        return ResponseEntity.ok(resourceAdminService.updateStatus(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping(AdminEndpoints.ADMIN_RESOURCE_BY_ID)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        resourceAdminService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
