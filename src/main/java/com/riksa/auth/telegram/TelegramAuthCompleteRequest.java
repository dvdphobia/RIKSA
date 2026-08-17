package com.riksa.auth.telegram;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TelegramAuthCompleteRequest(
        @NotBlank(message = "Telegram request token is required")
        @Pattern(
                regexp = "[A-Za-z0-9_-]{43}",
                message = "Telegram request token is invalid"
        )
        String requestToken,

        @NotBlank(message = "OTP code is required")
        @Pattern(regexp = "\\d{6}", message = "OTP code must contain 6 digits")
        String code
) {}

