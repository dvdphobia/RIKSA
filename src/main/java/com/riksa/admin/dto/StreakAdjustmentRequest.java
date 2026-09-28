package com.riksa.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StreakAdjustmentRequest {
    @NotNull
    @Min(0)
    private Long currentStreak;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
