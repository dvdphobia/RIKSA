package com.riksa.admin.controller;

import com.riksa.admin.dto.RoleUpdateRequest;
import com.riksa.admin.dto.SuspensionRequest;
import com.riksa.admin.service.UserAdminService;
import com.riksa.auth.dto.UserProfileResponse;
import com.riksa.common.constants.AdminEndpoints;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.USERS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @PostMapping(AdminEndpoints.USER_ROLE)
    public ResponseEntity<UserProfileResponse> updateRole(
            @PathVariable UUID id,
            @RequestBody @Valid RoleUpdateRequest request) {
        return ResponseEntity.ok(userAdminService.updateRole(getCurrentUserId(), id, request.getRole()));
    }

    @PostMapping(AdminEndpoints.USER_SUSPENSION)
    public ResponseEntity<UserProfileResponse> suspend(
            @PathVariable UUID id,
            @RequestBody @Valid SuspensionRequest request) {
        return ResponseEntity.ok(userAdminService.suspend(getCurrentUserId(), id, request));
    }

    @DeleteMapping(AdminEndpoints.USER_SUSPENSION)
    public ResponseEntity<UserProfileResponse> unsuspend(@PathVariable UUID id) {
        return ResponseEntity.ok(userAdminService.unsuspend(getCurrentUserId(), id));
    }

    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userAdminService.list(page, size));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}
