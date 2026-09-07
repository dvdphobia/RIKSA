package com.riksa.learning.dto;

import com.riksa.common.enums.DifficultyLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ExerciseResponse {
    private UUID id;
    private String question;
    private DifficultyLevel difficulty;
    private int points;
    private boolean completed;
}
