package com.riksa.resource.controller;

import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.gamification.dto.CursorPage;
import com.riksa.resource.ResourceEndpoints;
import com.riksa.resource.dto.CreateResourceRequest;
import com.riksa.resource.dto.ResourceResponse;
import com.riksa.resource.service.LessonResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LessonResourceController {
    private final LessonResourceService resourceService;

    @GetMapping(ResourceEndpoints.LESSON_RESOURCES)
    public CursorPage<ResourceResponse> list(
            @PathVariable UUID lessonId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return resourceService.list(lessonId, currentUserIdOrNull(), cursor, limit);
    }

    @PostMapping(ResourceEndpoints.LESSON_RESOURCES)
    public ResponseEntity<ResourceResponse> create(
            @PathVariable UUID lessonId,
            @RequestBody @Valid CreateResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.create(requireUserId(), lessonId, request));
    }

    @PostMapping(ResourceEndpoints.RESOURCE_SAVE)
    public ResourceResponse save(@PathVariable UUID resourceId) {
        return resourceService.save(requireUserId(), resourceId);
    }

    @DeleteMapping(ResourceEndpoints.RESOURCE_SAVE)
    public ResourceResponse unsave(@PathVariable UUID resourceId) {
        return resourceService.unsave(requireUserId(), resourceId);
    }

    @DeleteMapping(ResourceEndpoints.RESOURCE_BY_ID)
    public ResponseEntity<Void> delete(@PathVariable UUID resourceId) {
        resourceService.delete(requireUserId(), resourceId);
        return ResponseEntity.noContent().build();
    }

    private UUID requireUserId() {
        UUID userId = currentUserIdOrNull();
        if (userId == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        return userId;
    }

    private UUID currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
