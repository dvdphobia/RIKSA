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

    public AnswerResult(UUID exerciseId, boolean correct, int points, String message) {
        this.exerciseId = exerciseId;
        this.correct = correct;
        this.points = points;
        this.message = message;
    }
}