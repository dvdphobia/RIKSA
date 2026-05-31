package com.riksa.auth.otp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpSendRequest {

    @NotBlank(message = "Identifier is required (phone number or Telegram chat ID)")
    private String identifier;

    @NotNull(message = "Channel is required (SMS or TELEGRAM)")
    private OtpChannel channel;
}
