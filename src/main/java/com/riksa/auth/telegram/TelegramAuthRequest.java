package com.riksa.auth.telegram;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TelegramAuthRequest {

    @NotBlank(message = "Telegram chat ID is required")
    @Pattern(regexp = "\\d+", message = "Telegram chat ID must be numeric")
    private String identifier;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "\\d{6}", message = "OTP code must contain 6 digits")
    private String code;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
    private String displayName;
}
