package com.riksa.learning.dto;

import com.riksa.common.enums.DifficultyLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class LessonListResponse {
    private UUID id;
    private String title;
    private String description;
    private DifficultyLevel difficulty;
    private int baseXp;
    private int position;
    private UUID categoryId;
    private String categoryName;
    private int exerciseCount;
    private boolean completed;
    private LessonProgressState state;
    private OffsetDateTime createdAt;
}
