package com.riksa.blog.controller;

import com.riksa.blog.dto.*;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.service.BlogEngagementService;
import com.riksa.blog.service.BlogModerationService;
import com.riksa.blog.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;
    private final BlogEngagementService blogEngagementService;
    private final BlogModerationService blogModerationService;

    @GetMapping
    public ResponseEntity<BlogListResponse> listPosts(
            @RequestParam(defaultValue = "APPROVED") BlogStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = getCurrentUserId();
        var pageResult = blogService.listPosts(status, categoryId, page, size);

        var content = pageResult.getContent().stream()
                .map(post -> BlogMapper.toResponse(post,
                        blogEngagementService.isLiked(post.getId(), userId)))
                .toList();

        return ResponseEntity.ok(new BlogListResponse(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        ));
    }

    @PostMapping
    public ResponseEntity<BlogPostResponse> createPost(@RequestBody BlogPostRequest request) {
        UUID userId = getCurrentUserId();
        var post = blogService.createPost(
                userId,
                request.title(),
                request.content(),
                request.coverUrl(),
                request.categoryId()
        );
        return ResponseEntity.ok(BlogMapper.toResponse(post, false));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogPostResponse> getPost(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        var post = blogService.getPost(id);
        blogEngagementService.trackView(id, userId);
        return ResponseEntity.ok(BlogMapper.toResponse(post,
                blogEngagementService.isLiked(id, userId)));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<BlogPostResponse> toggleLike(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        var post = blogEngagementService.toggleLike(id, userId);
        return ResponseEntity.ok(BlogMapper.toResponse(post,
                blogEngagementService.isLiked(id, userId)));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> trackView(@PathVariable UUID id) {
        UUID userId = getCurrentUserIdOrNull();
        blogEngagementService.trackView(id, userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<BlogPostResponse> moderatePost(
            @PathVariable UUID id,
            @RequestParam BlogStatus status,
            @RequestBody(required = false) ModerationRequest request) {

        var post = status == BlogStatus.APPROVED
                ? blogModerationService.approve(id)
                : blogModerationService.reject(id, request != null ? request.reason() : null);

        return ResponseEntity.ok(BlogMapper.toResponse(post, false));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }

    private UUID getCurrentUserIdOrNull() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            return UUID.fromString(auth.getName());
        } catch (Exception e) {
            return null;
        }
    }
}