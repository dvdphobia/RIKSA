package com.riksa.admin.dto;

import com.riksa.common.enums.DifficultyLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class LessonCreateRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private DifficultyLevel difficulty;

    @Min(0)
    private int baseXp = 10;

    @Min(1)
    private Integer position;

    private UUID categoryId;

    private String reflectionPrompt;

    @NotEmpty
    @Valid
    private List<ExerciseRequest> exercises;
}
