package com.riksa.admin.dto.settings;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiChatSettings(
        @NotNull Boolean enabled,
        @NotNull @Min(0) Integer dailyMessageLimit,
        @NotNull @Size(max = 8000) String systemPrompt) {
}
