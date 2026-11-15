package com.riksa.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class LessonResponse {
    private UUID id;
    private String title;
    private String description;
    private String difficulty;
    private int baseXp;
    private int position;
    private int estimatedMinutes;
    private UUID categoryId;
    private UUID roadmapUnitId;
    private String reflectionPrompt;
    private String backgroundKey;
    private String backgroundImageUrl;
    private List<ExerciseResponse> exercises;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
