package com.riksa.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RoadmapUnitResponse(
        UUID id,
        UUID categoryId,
        String title,
        String description,
        int position,
        long lessonCount,
        OffsetDateTime updatedAt
) {}
