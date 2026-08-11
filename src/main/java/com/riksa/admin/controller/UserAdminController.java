package com.riksa.admin.controller;

import com.riksa.admin.dto.AdminUserResponse;
import com.riksa.admin.dto.RoleUpdateRequest;
import com.riksa.admin.dto.SuspensionRequest;
import com.riksa.admin.service.UserAdminService;
import com.riksa.auth.entity.Role;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.USERS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
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
