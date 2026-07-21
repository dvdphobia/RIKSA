package com.riksa.learning.dto;

import com.riksa.common.enums.DifficultyLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class LessonDetailResponse {
    private UUID id;
    private String title;
    private String description;
    private DifficultyLevel difficulty;
    private int baseXp;
    private UUID categoryId;
    private String categoryName;
    private String reflectionPrompt;
    private List<ExerciseResponse> exercises;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}