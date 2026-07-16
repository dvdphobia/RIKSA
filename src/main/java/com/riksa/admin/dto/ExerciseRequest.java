package com.riksa.admin.dto;

import com.riksa.common.enums.DifficultyLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

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
}