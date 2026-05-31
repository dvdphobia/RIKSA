package com.riksa.auth.otp;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpSendRequest {

    @NotBlank(message = "Telegram chat ID is required")
    private String identifier;
}
