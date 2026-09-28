package com.riksa.admin.dto;

import com.riksa.resource.entity.ResourceKind;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminLessonResource(
        UUID id,
        UUID lessonId,
        String lessonTitle,
        UUID authorId,
        String authorUsername,
        String title,
        String url,
        ResourceKind kind,
        boolean hidden,
        OffsetDateTime hiddenAt,
        UUID hiddenBy,
        long saveCount,
        OffsetDateTime createdAt
) {}
