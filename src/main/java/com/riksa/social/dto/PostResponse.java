package com.riksa.social.dto;

import com.riksa.social.enums.ReactionType;
import com.riksa.social.enums.PostKind;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PostResponse(
        UUID id,
        String title,
        String content,
        String mediaUrl,
        int reactionCount,
        int commentCount,
        int shareCount,
        UserResponse author,
        CategoryResponse category,
        ReactionType myReaction,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        PostKind kind,
        MilestoneResponse milestone
) {}
