package com.riksa.blog.controller;

import com.riksa.blog.dto.*;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.service.BlogEngagementService;
import com.riksa.blog.service.BlogService;
import com.riksa.common.constants.BlogEndpoints;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(BlogEndpoints.BASE)
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;
    private final BlogEngagementService blogEngagementService;

    @GetMapping(BlogEndpoints.ROOT)
    public ResponseEntity<BlogListResponse> listPosts(
            @RequestParam(defaultValue = "APPROVED") BlogStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = getCurrentUserId();
        // Non-moderators can only see APPROVED posts
        boolean isModerator = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MODERATOR")
                        || a.getAuthority().equals("ROLE_ADMIN"));
        BlogStatus effectiveStatus = isModerator ? status : BlogStatus.APPROVED;
        int effectiveSize = Math.min(Math.max(size, 1), 100);
        var pageResult = blogService.listPosts(effectiveStatus, categoryId, page, effectiveSize);

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

    @PostMapping(BlogEndpoints.ROOT)
    public ResponseEntity<BlogPostResponse> createPost(@RequestBody @Valid BlogPostRequest request) {
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

    @GetMapping(BlogEndpoints.MY_POSTS)
    public ResponseEntity<BlogListResponse> getMyPosts(
            @RequestParam(required = false) BlogStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = getCurrentUserId();
        int effectivePage = Math.max(page, 0);
        int effectiveSize = Math.min(Math.max(size, 1), 100);
        var pageResult = blogService.getMyPosts(userId, status, effectivePage, effectiveSize);

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

    @GetMapping(BlogEndpoints.POST)
    public ResponseEntity<BlogPostResponse> getPost(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        var post = blogService.getPost(id);
        // Non-moderators can only see APPROVED posts
        boolean isModerator = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MODERATOR")
                        || a.getAuthority().equals("ROLE_ADMIN"));
        if (post.getStatus() != BlogStatus.APPROVED && !isModerator) {
            throw new IllegalArgumentException("Blog post not found: " + id);
        }
        blogEngagementService.trackView(id, userId);
        return ResponseEntity.ok(BlogMapper.toResponse(post,
                blogEngagementService.isLiked(id, userId)));
    }

    @PostMapping(BlogEndpoints.LIKE)
    public ResponseEntity<BlogPostResponse> toggleLike(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        var post = blogEngagementService.toggleLike(id, userId);
        return ResponseEntity.ok(BlogMapper.toResponse(post,
                blogEngagementService.isLiked(id, userId)));
    }

    @PostMapping(BlogEndpoints.VIEW)
    public ResponseEntity<Void> trackView(@PathVariable UUID id) {
        UUID userId = getCurrentUserIdOrNull();
        blogEngagementService.trackView(id, userId);
        return ResponseEntity.ok().build();
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
