package com.riksa.learning.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AnswerResult {
    private UUID exerciseId;
    private boolean correct;
    private int points;
    private String message;
    private String hint;
    private String explanation;
    private int attemptNumber;

    public AnswerResult(UUID exerciseId, boolean correct, int points, String message,
                        String hint, String explanation, int attemptNumber) {
        this.exerciseId = exerciseId;
        this.correct = correct;
        this.points = points;
        this.message = message;
        this.hint = hint;
        this.explanation = explanation;
        this.attemptNumber = attemptNumber;
    }
}
