package com.riksa.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ExerciseResponse {
    private UUID id;
    private String question;
    private String difficulty;
    private int points;

    public ExerciseResponse(UUID id, String question, String difficulty, int points) {
        this.id = id;
        this.question = question;
        this.difficulty = difficulty;
        this.points = points;
    }
}