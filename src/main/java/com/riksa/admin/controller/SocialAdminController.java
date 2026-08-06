package com.riksa.admin.controller;

import com.riksa.admin.dto.ContentModerationResponse;
import com.riksa.admin.dto.ModerationRequest;
import com.riksa.admin.service.SocialModerationService;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.social.enums.ContentStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(AdminEndpoints.BASE)
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@RequiredArgsConstructor
public class SocialAdminController {

    private final SocialModerationService socialModerationService;

    @PatchMapping(AdminEndpoints.POSTS + AdminEndpoints.POST_STATUS)
    public ResponseEntity<ContentModerationResponse> moderatePost(
            @PathVariable UUID id,
            @RequestBody @Valid ModerationRequest request) {
        return ResponseEntity.ok(socialModerationService.setPostStatus(
                getCurrentUserId(), id, parseStatus(request.getStatus()), request.getReason()));
    }

    @PatchMapping(AdminEndpoints.COMMENTS + AdminEndpoints.COMMENT_STATUS)
    public ResponseEntity<ContentModerationResponse> moderateComment(
            @PathVariable UUID id,
            @RequestBody @Valid ModerationRequest request) {
        return ResponseEntity.ok(socialModerationService.setCommentStatus(
                getCurrentUserId(), id, parseStatus(request.getStatus()), request.getReason()));
    }

    private ContentStatus parseStatus(String status) {
        try {
            return ContentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status + ". Valid values: HIDDEN, VISIBLE");
        }
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}
