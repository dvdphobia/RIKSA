package com.riksa.blog.dto;

import com.riksa.blog.enums.BlogStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BlogPostResponse(
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
        OffsetDateTime updatedAt
) {}