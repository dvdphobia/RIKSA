package com.riksa.admin.controller;

import com.riksa.admin.dto.*;
import com.riksa.admin.service.LessonAdminService;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.common.enums.DifficultyLevel;
import com.riksa.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
        LessonResponse response = lessonAdminService.create(SecurityUtils.currentUserId(), request);
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
        return ResponseEntity.ok(lessonAdminService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping(AdminEndpoints.LESSON_BY_ID)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        lessonAdminService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(AdminEndpoints.LESSON_BULK)
    public ResponseEntity<BulkLessonResult> bulkCreate(@RequestBody @Valid List<LessonCreateRequest> requests) {
        return ResponseEntity.ok(lessonAdminService.bulkCreate(SecurityUtils.currentUserId(), requests));
    }

    @PostMapping(value = AdminEndpoints.LESSON_UPLOAD, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LessonResponse> upload(@RequestParam("file") MultipartFile file) {
        LessonResponse response = lessonAdminService.uploadMarkdown(SecurityUtils.currentUserId(), file);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(AdminEndpoints.LESSON_BY_ID)
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping(value = AdminEndpoints.LESSON_UPLOAD_BULK, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkLessonResult> uploadBulk(@RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(lessonAdminService.uploadMarkdownBulk(SecurityUtils.currentUserId(), files));
    }

    @GetMapping(AdminEndpoints.ROOT)
    public ResponseEntity<Page<LessonResponse>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(lessonAdminService.list(page, size, query, difficulty, categoryId));
    }

    @GetMapping(AdminEndpoints.LESSON_BY_ID)
    public ResponseEntity<LessonResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(lessonAdminService.get(id));
    }

    @PutMapping(AdminEndpoints.LESSON_REORDER)
    public ResponseEntity<Void> reorder(@RequestBody @Valid LessonReorderRequest request) {
        lessonAdminService.reorder(SecurityUtils.currentUserId(), request);
        return ResponseEntity.noContent().build();
    }
}
