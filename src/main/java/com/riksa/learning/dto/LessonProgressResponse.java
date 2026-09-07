package com.riksa.learning.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LessonProgressResponse(
        UUID lessonId,
        String status,
        OffsetDateTime lastAccessedAt
) {}
