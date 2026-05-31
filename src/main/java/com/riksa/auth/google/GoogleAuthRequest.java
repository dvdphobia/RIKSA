package com.riksa.auth.google;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class GoogleAuthRequest {

    @NotBlank(message = "Google ID token is required")
    private String idToken;

    private String username;
    private String displayName;
    private UUID provinceId;
}


