package com.riksa.auth.telegram;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Sign-in code the user copied from the bot's /start or /code reply. */
public record TelegramCodeRequest(
        @NotBlank(message = "Sign-in code is required")
        @Size(max = 32, message = "Sign-in code is invalid")
        String code
) {}
