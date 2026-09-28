package com.riksa.admin.controller;

import com.riksa.admin.dto.CommentModerationResponse;
import com.riksa.admin.dto.ContentModerationRequest;
import com.riksa.admin.dto.ContentModerationResponse;
import com.riksa.admin.dto.PostModerationResponse;
import com.riksa.admin.service.SocialModerationService;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.common.security.SecurityUtils;
import com.riksa.social.enums.ContentStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(AdminEndpoints.BASE)
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@RequiredArgsConstructor
public class SocialAdminController {

    private final SocialModerationService socialModerationService;

    @GetMapping(AdminEndpoints.POSTS)
    public ResponseEntity<Page<PostModerationResponse>> listPosts(
            @RequestParam(defaultValue = "HIDDEN") ContentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(socialModerationService.listPosts(status, page, size));
    }

    @GetMapping(AdminEndpoints.COMMENTS)
    public ResponseEntity<Page<CommentModerationResponse>> listComments(
            @RequestParam(defaultValue = "HIDDEN") ContentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(socialModerationService.listComments(status, page, size));
    }

    @PatchMapping(AdminEndpoints.POSTS + AdminEndpoints.POST_STATUS)
    public ResponseEntity<ContentModerationResponse> moderatePost(
            @PathVariable UUID id,
            @RequestBody @Valid ContentModerationRequest request) {
        return ResponseEntity.ok(socialModerationService.setPostStatus(
                SecurityUtils.currentUserId(), id, request.getStatus(), request.getReason()));
    }

    @PatchMapping(AdminEndpoints.COMMENTS + AdminEndpoints.COMMENT_STATUS)
    public ResponseEntity<ContentModerationResponse> moderateComment(
            @PathVariable UUID id,
            @RequestBody @Valid ContentModerationRequest request) {
        return ResponseEntity.ok(socialModerationService.setCommentStatus(
                SecurityUtils.currentUserId(), id, request.getStatus(), request.getReason()));
    }

    @DeleteMapping(AdminEndpoints.POSTS + AdminEndpoints.POST_MEDIA)
    public ResponseEntity<Void> removePostMedia(
            @PathVariable UUID id,
            @RequestParam String reason) {
        socialModerationService.removePostMedia(SecurityUtils.currentUserId(), id, reason);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(AdminEndpoints.COMMENTS + AdminEndpoints.COMMENT_MEDIA)
    public ResponseEntity<Void> removeCommentMedia(
            @PathVariable UUID id,
            @RequestParam String reason) {
        socialModerationService.removeCommentMedia(SecurityUtils.currentUserId(), id, reason);
        return ResponseEntity.noContent().build();
    }
}
