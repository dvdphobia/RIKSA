package com.riksa.learning.dto;

import com.riksa.common.enums.DifficultyLevel;

import java.util.UUID;

public record RoadmapLessonResponse(
        UUID id,
        String title,
        String description,
        DifficultyLevel difficulty,
        int baseXp,
        int rewardXp,
        int estimatedMinutes,
        int exerciseCount,
        double progress,
        String status,
        boolean current
) {}
