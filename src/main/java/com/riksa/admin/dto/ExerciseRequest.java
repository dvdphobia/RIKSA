package com.riksa.admin.dto;

import com.riksa.common.enums.DifficultyLevel;
import com.riksa.learning.entity.ExerciseType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ExerciseRequest {
    @NotBlank
    private String question;

    @NotBlank
    private String answerKey;

    @NotNull
    private DifficultyLevel difficulty;

    @Min(1)
    private int points = 5;

    @Min(1)
    private int sectionIndex = 1;

    @NotNull
    private ExerciseType type = ExerciseType.TEXT;

    private List<String> options;

    private String hint;

    private String explanation;
}
