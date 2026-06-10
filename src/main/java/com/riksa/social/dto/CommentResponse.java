package com.riksa.social.dto;

import com.riksa.social.enums.ReactionType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String content,
        UserResponse author,
        UUID postId,
        UUID parentId,
        ReactionType myReaction,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}