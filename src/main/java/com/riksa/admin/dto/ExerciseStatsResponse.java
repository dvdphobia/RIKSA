package com.riksa.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ExerciseStatsResponse {
    private UUID exerciseId;
    private String question;
    private String difficulty;
    private long attempts;
    private long correctCount;
    private double correctRate;
    private double failureRate;
}
