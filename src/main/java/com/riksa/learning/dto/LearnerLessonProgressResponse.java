package com.riksa.learning.dto;

import com.riksa.common.enums.DifficultyLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
public class LearnerLessonProgressResponse {
    private UUID lessonId;
    private String lessonTitle;
    private UUID categoryId;
    private String categoryName;
    private LessonProgressState roadmapState;
    private int totalExerciseCount;
    private int attemptedExerciseCount;
    private int correctExerciseCount;
    private double progressPercentage;
    private int currentSectionIndex;
    private OffsetDateTime lastActivityAt;
    private boolean completed;
    private OffsetDateTime completedAt;
    private boolean canResume;
    private DifficultyLevel difficulty;
    private int baseXp;
    private int position;
}
