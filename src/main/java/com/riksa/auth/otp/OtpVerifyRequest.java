package com.riksa.auth.otp;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpVerifyRequest {

    @NotBlank(message = "Telegram chat ID is required")
    private String identifier;

    @NotBlank(message = "OTP code is required")
    private String code;
}
