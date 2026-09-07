package com.riksa.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.List;
import com.riksa.learning.entity.ExerciseType;

@Getter
@Setter
public class ExerciseResponse {
    private UUID id;
    private String question;
    private String answerKey;
    private String difficulty;
    private int points;
    private int sectionIndex;
    private ExerciseType type;
    private List<String> options;
    private String hint;
    private String explanation;

    public ExerciseResponse(UUID id, String question, String answerKey, String difficulty, int points, int sectionIndex,
                            ExerciseType type, List<String> options, String hint, String explanation) {
        this.id = id;
        this.question = question;
        this.answerKey = answerKey;
        this.difficulty = difficulty;
        this.points = points;
        this.sectionIndex = sectionIndex;
        this.type = type;
        this.options = options;
        this.hint = hint;
        this.explanation = explanation;
    }
}
