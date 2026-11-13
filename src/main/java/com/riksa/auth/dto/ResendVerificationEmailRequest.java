package com.riksa.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendVerificationEmailRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "A valid email address is required")
    private String email;
}
