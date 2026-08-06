package com.riksa.admin.dto;

import com.riksa.common.enums.DifficultyLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * All fields are optional — null means "don't change".
 * {@code exercises} is a special case: if present, it replaces all existing
 * exercises; if null, exercises are left untouched.
 */
@Getter
@Setter
public class LessonUpdateRequest {
    private String title;
    private String description;
    private DifficultyLevel difficulty;
    @Min(0)
    private Integer baseXp;
    private UUID categoryId;
    private String reflectionPrompt;

    @Valid
    @NotEmpty
    private List<ExerciseRequest> exercises;
}