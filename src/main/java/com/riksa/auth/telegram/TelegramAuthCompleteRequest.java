package com.riksa.auth.telegram;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Creates the Riksa account for a confirmed Telegram user who has none yet. */
public record TelegramAuthCompleteRequest(
        @NotBlank(message = "Signup token is required")
        @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Signup token is invalid")
        String signupToken,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Display name is required")
        @Size(max = 100, message = "Display name must be at most 100 characters")
        String displayName
) {}
