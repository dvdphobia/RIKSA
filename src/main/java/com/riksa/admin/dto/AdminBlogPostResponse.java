package com.riksa.admin.dto;

import com.riksa.blog.enums.BlogStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Blog response for moderator and admin endpoints, including internal moderation signals. */
public record AdminBlogPostResponse(
        UUID id,
        UUID authorId,
        String title,
        String content,
        String coverUrl,
        BlogStatus status,
        String rejectionReason,
        int viewCount,
        int likeCount,
        UUID categoryId,
        boolean liked,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Integer moderationScore,
        String moderationReasons
) {}
