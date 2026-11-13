package com.riksa.admin.controller;

import com.riksa.admin.dto.BlogModerationRequest;
import com.riksa.admin.dto.AdminBlogListResponse;
import com.riksa.admin.dto.AdminBlogPostResponse;
import com.riksa.admin.mapper.AdminBlogMapper;
import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.service.BlogModerationService;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.common.security.SecurityUtils;
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

    @GetMapping
    public ResponseEntity<AdminBlogListResponse> list(
            @RequestParam(defaultValue = "PENDING") BlogStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = blogModerationService.list(status, page, size);
        var content = result.getContent().stream()
                .map(post -> AdminBlogMapper.toResponse(post, false))
                .toList();
        return ResponseEntity.ok(new AdminBlogListResponse(
                content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminBlogPostResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(AdminBlogMapper.toResponse(blogModerationService.getPost(id), false));
    }

    @PatchMapping(AdminEndpoints.BLOG_STATUS)
    public ResponseEntity<AdminBlogPostResponse> moderate(
            @PathVariable UUID id,
            @RequestBody @Valid BlogModerationRequest request) {

        UUID moderatorId = SecurityUtils.currentUserId();

        if (request.getStatus() == BlogStatus.APPROVED) {
            BlogPost post = blogModerationService.approve(id, moderatorId);
            return ResponseEntity.ok(AdminBlogMapper.toResponse(post, false));
        } else if (request.getStatus() == BlogStatus.HIDDEN) {
            BlogPost post = blogModerationService.hide(id, request.getReason(), moderatorId);
            return ResponseEntity.ok(AdminBlogMapper.toResponse(post, false));
        } else if (request.getStatus() == BlogStatus.REJECTED) {
            BlogPost post = blogModerationService.reject(id, request.getReason(), moderatorId);
            return ResponseEntity.ok(AdminBlogMapper.toResponse(post, false));
        } else {
            throw new IllegalArgumentException("Cannot set status to PENDING via moderation endpoint");
        }
    }
}
