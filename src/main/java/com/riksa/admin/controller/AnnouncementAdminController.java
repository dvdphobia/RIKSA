package com.riksa.admin.controller;

import com.riksa.admin.dto.AnnouncementRequest;
import com.riksa.admin.dto.AnnouncementResponse;
import com.riksa.admin.service.AnnouncementAdminService;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.ADMIN_ANNOUNCEMENTS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AnnouncementAdminController {

    private final AnnouncementAdminService announcementAdminService;

    @GetMapping
    public ResponseEntity<List<AnnouncementResponse>> list() {
        return ResponseEntity.ok(announcementAdminService.list());
    }

    @PostMapping
    public ResponseEntity<AnnouncementResponse> create(@RequestBody @Valid AnnouncementRequest request) {
        AnnouncementResponse response = announcementAdminService.create(SecurityUtils.currentUserId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path(AdminEndpoints.ANNOUNCEMENT_BY_ID)
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping(AdminEndpoints.ANNOUNCEMENT_BY_ID)
    public ResponseEntity<AnnouncementResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid AnnouncementRequest request) {
        return ResponseEntity.ok(announcementAdminService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping(AdminEndpoints.ANNOUNCEMENT_BY_ID)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        announcementAdminService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
