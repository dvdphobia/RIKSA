package com.riksa.admin.dto.settings;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record XpRulesSettings(@NotNull @Min(0) Integer dailyLessonXpCap) {
}
