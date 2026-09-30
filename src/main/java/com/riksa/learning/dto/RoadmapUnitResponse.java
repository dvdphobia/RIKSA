package com.riksa.learning.dto;

import java.util.List;
import java.util.UUID;

public record RoadmapUnitResponse(
        UUID id,
        String title,
        String description,
        int position,
        String status,
        int completedLessons,
        int totalLessons,
        boolean skippable,
        List<RoadmapLessonResponse> lessons
) {}
