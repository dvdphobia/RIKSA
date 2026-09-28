package com.riksa.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RankTierUpdateRequest(@Min(0) long minXp, @NotBlank String iconUrl) {
}
