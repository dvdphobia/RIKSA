package com.riksa.admin.controller;

import com.riksa.admin.dto.CategoryRequest;
import com.riksa.admin.dto.CategoryResponse;
import com.riksa.admin.service.CategoryAdminService;
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
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.CATEGORIES)
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@RequiredArgsConstructor
public class CategoryAdminController {

    private final CategoryAdminService categoryAdminService;

    @GetMapping(AdminEndpoints.ROOT)
    public ResponseEntity<List<CategoryResponse>> list() {
        return ResponseEntity.ok(categoryAdminService.list());
    }

    @GetMapping(AdminEndpoints.CATEGORY_BY_ID)
    public ResponseEntity<CategoryResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryAdminService.get(id));
    }

    @PostMapping(AdminEndpoints.ROOT)
    public ResponseEntity<CategoryResponse> create(@RequestBody @Valid CategoryRequest request) {
        CategoryResponse response = categoryAdminService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(AdminEndpoints.CATEGORY_BY_ID)
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping(AdminEndpoints.CATEGORY_BY_ID)
    public ResponseEntity<CategoryResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid CategoryRequest request) {
        return ResponseEntity.ok(categoryAdminService.update(id, request));
    }

    @DeleteMapping(AdminEndpoints.CATEGORY_BY_ID)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
