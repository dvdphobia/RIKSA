package com.riksa.admin.controller;

import com.riksa.admin.dto.ModerationRequest;
import com.riksa.auth.security.JwtService;
import com.riksa.blog.dto.BlogMapper;
import com.riksa.blog.dto.BlogPostResponse;
import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.service.BlogModerationService;
import com.riksa.common.constants.AdminEndpoints;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.BLOG)
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@RequiredArgsConstructor
public class BlogAdminController {

    private final BlogModerationService blogModerationService;
    private final JwtService jwtService;

    @PatchMapping(AdminEndpoints.BLOG_STATUS)
    public ResponseEntity<BlogPostResponse> moderate(
            @PathVariable UUID id,
            @RequestBody @Valid ModerationRequest request,
            @RequestHeader("Authorization") String authHeader) {

        UUID moderatorId = extractUserId(authHeader);

        BlogStatus status;
        try {
            status = BlogStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status: " + request.getStatus() + ". Valid values: APPROVED, REJECTED");
        }

        if (status == BlogStatus.APPROVED) {
            BlogPost post = blogModerationService.approve(id, moderatorId);
            return ResponseEntity.ok(BlogMapper.toResponse(post, false));
        } else if (status == BlogStatus.REJECTED) {
            BlogPost post = blogModerationService.reject(id, request.getReason(), moderatorId);
            return ResponseEntity.ok(BlogMapper.toResponse(post, false));
        } else {
            throw new IllegalArgumentException("Cannot set status to PENDING via moderation endpoint");
        }
    }

    private UUID extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        return jwtService.getUserId(authHeader.substring(7));
    }
}
