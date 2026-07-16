package com.riksa.admin.controller;

import com.riksa.admin.dto.RoleUpdateRequest;
import com.riksa.admin.service.UserAdminService;
import com.riksa.auth.dto.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @PostMapping("/{id}/role")
    public ResponseEntity<UserProfileResponse> updateRole(
            @PathVariable UUID id,
            @RequestBody @Valid RoleUpdateRequest request) {
        return ResponseEntity.ok(userAdminService.updateRole(id, request.getRole()));
    }
}