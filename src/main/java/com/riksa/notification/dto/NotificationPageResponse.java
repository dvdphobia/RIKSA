package com.riksa.notification.dto;

import java.util.List;

/** A page of the inbox, newest first. Pass {@code nextCursor} back as {@code cursor}. */
public record NotificationPageResponse(
        List<NotificationResponse> items,
        long unreadCount,
        String nextCursor
) {}
