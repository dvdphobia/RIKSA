package com.riksa.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StreakMilestoneRequest(
        @Min(1) long minStreak,
        @NotBlank @Size(max = 100) String streakName,
        @NotBlank String iconUrl) {
}
