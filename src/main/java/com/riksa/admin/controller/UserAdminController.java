package com.riksa.admin.controller;

import com.riksa.admin.dto.RoleUpdateRequest;
import com.riksa.admin.service.UserAdminService;
import com.riksa.auth.dto.UserProfileResponse;
import com.riksa.common.constants.AdminEndpoints;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
        return ResponseEntity.ok(userAdminService.updateRole(id, request.getRole()));
    }

    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userAdminService.list(page, size));
    }
}