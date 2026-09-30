package com.riksa.learning.dto;

import com.riksa.common.enums.DifficultyLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;
import java.util.List;
import com.riksa.learning.entity.ExerciseType;

@Getter
@Setter
@NoArgsConstructor
public class ExerciseResponse {
    private UUID id;
    private String question;
    private DifficultyLevel difficulty;
    private int points;
    private int sectionIndex;
    private ExerciseType type;
    private List<String> options;
    private String hint;
    private String explanation;
    private boolean completed;
}
