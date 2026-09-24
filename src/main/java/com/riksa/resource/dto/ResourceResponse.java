package com.riksa.resource.dto;

import com.riksa.resource.entity.ResourceKind;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResourceResponse(
        UUID id,
        UUID lessonId,
        String title,
        String url,
        ResourceKind kind,
        ResourceAuthorResponse author,
        long saveCount,
        boolean savedByMe,
        OffsetDateTime createdAt
) {}
