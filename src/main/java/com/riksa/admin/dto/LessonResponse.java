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
    private UUID categoryId;
    private List<ExerciseResponse> exercises;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}