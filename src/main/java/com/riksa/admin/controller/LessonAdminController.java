package com.riksa.admin.controller;

import com.riksa.admin.dto.*;
import com.riksa.admin.service.LessonAdminService;
import com.riksa.common.constants.AdminEndpoints;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.LESSONS)
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@RequiredArgsConstructor
public class LessonAdminController {

    private final LessonAdminService lessonAdminService;

    @PostMapping(AdminEndpoints.ROOT)
    public ResponseEntity<LessonResponse> create(@RequestBody @Valid LessonCreateRequest request) {
        LessonResponse response = lessonAdminService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(AdminEndpoints.LESSON_BY_ID)
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping(AdminEndpoints.LESSON_BY_ID)
    public ResponseEntity<LessonResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid LessonUpdateRequest request) {
        return ResponseEntity.ok(lessonAdminService.update(id, request));
    }

    @DeleteMapping(AdminEndpoints.LESSON_BY_ID)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        lessonAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(AdminEndpoints.LESSON_BULK)
    public ResponseEntity<BulkLessonResult> bulkCreate(@RequestBody @Valid List<LessonCreateRequest> requests) {
        return ResponseEntity.ok(lessonAdminService.bulkCreate(requests));
    }

    @GetMapping(AdminEndpoints.ROOT)
    public ResponseEntity<List<LessonResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(lessonAdminService.list(page, size));
    }
}
