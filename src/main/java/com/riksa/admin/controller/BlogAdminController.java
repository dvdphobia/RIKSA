package com.riksa.admin.controller;

import com.riksa.admin.dto.BlogModerationRequest;
import com.riksa.blog.dto.BlogListResponse;
import com.riksa.blog.dto.BlogMapper;
import com.riksa.blog.dto.BlogPostResponse;
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
    public ResponseEntity<BlogListResponse> list(
            @RequestParam(defaultValue = "PENDING") BlogStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = blogModerationService.list(status, page, size);
        var content = result.getContent().stream()
                .map(post -> BlogMapper.toResponse(post, false))
                .toList();
        return ResponseEntity.ok(new BlogListResponse(
                content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }

    @PatchMapping(AdminEndpoints.BLOG_STATUS)
    public ResponseEntity<BlogPostResponse> moderate(
            @PathVariable UUID id,
            @RequestBody @Valid BlogModerationRequest request) {

        UUID moderatorId = SecurityUtils.currentUserId();

        if (request.getStatus() == BlogStatus.APPROVED) {
            BlogPost post = blogModerationService.approve(id, moderatorId);
            return ResponseEntity.ok(BlogMapper.toResponse(post, false));
        } else if (request.getStatus() == BlogStatus.REJECTED) {
            BlogPost post = blogModerationService.reject(id, request.getReason(), moderatorId);
            return ResponseEntity.ok(BlogMapper.toResponse(post, false));
        } else {
            throw new IllegalArgumentException("Cannot set status to PENDING via moderation endpoint");
        }
    }
}
