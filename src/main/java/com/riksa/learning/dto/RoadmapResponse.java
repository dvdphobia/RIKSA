package com.riksa.learning.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RoadmapResponse(
        UUID categoryId,
        String categoryName,
        int completedLessons,
        int totalLessons,
        double progress,
        UUID currentLessonId,
        List<LocalDate> activeDates,
        List<RoadmapUnitResponse> units
) {}
