package com.riksa.notification.controller;

import com.riksa.common.constants.NotificationEndpoints;
import com.riksa.common.security.SecurityUtils;
import com.riksa.notification.dto.NotificationPageResponse;
import com.riksa.notification.dto.UnreadCountResponse;
import com.riksa.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The signed-in learner's own inbox. */
@RestController
@RequestMapping(NotificationEndpoints.BASE)
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationPageResponse> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(notificationService.list(SecurityUtils.currentUserId(), cursor, limit));
    }

    @GetMapping(NotificationEndpoints.UNREAD_COUNT)
    public ResponseEntity<UnreadCountResponse> unreadCount() {
        return ResponseEntity.ok(new UnreadCountResponse(
                notificationService.unreadCount(SecurityUtils.currentUserId())));
    }

    @PostMapping(NotificationEndpoints.READ)
    public ResponseEntity<UnreadCountResponse> markRead(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(new UnreadCountResponse(
                notificationService.markRead(SecurityUtils.currentUserId(), id)));
    }

    @PostMapping(NotificationEndpoints.READ_ALL)
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
