package com.riksa.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateProfileRequest {

    private String displayName;
    private String bio;
    private String gender;
    private UUID provinceId;
}
