package com.riksa.learning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExerciseAttemptRequest {
    @NotBlank(message = "Answer is required")
    private String answer;
}