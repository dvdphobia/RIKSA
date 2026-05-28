package com.riksa.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendOtpRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+855|0)\\d{8,9}$", message = "Invalid Cambodian phone number format")
    private String phoneNumber;
}
