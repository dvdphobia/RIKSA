package com.riksa.admin.controller;

import com.riksa.admin.dto.*;
import com.riksa.admin.service.LessonAdminService;
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
@RequestMapping("/api/v1/admin/lessons")
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@RequiredArgsConstructor
public class LessonAdminController {

    private final LessonAdminService lessonAdminService;

    @PostMapping
    public ResponseEntity<LessonResponse> create(@RequestBody @Valid LessonCreateRequest request) {
        LessonResponse response = lessonAdminService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LessonResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid LessonUpdateRequest request) {
        return ResponseEntity.ok(lessonAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        lessonAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkLessonResult> bulkCreate(@RequestBody @Valid List<LessonCreateRequest> requests) {
        return ResponseEntity.ok(lessonAdminService.bulkCreate(requests));
    }

    @GetMapping
    public ResponseEntity<List<LessonResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(lessonAdminService.list(page, size));
    }
}
