package com.riksa.admin.controller;

import com.riksa.admin.dto.CategoryRequest;
import com.riksa.admin.dto.CategoryResponse;
import com.riksa.admin.service.CategoryAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@RequiredArgsConstructor
public class CategoryAdminController {

    private final CategoryAdminService categoryAdminService;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@RequestBody @Valid CategoryRequest request) {
        CategoryResponse response = categoryAdminService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid CategoryRequest request) {
        return ResponseEntity.ok(categoryAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
