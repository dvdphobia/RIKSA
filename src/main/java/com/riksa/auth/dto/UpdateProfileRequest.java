package com.riksa.auth.dto;

import jakarta.validation.constraints.Pattern;
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
    @Pattern(
            regexp = "/avatars/avatar-(?:[1-9]|[12][0-9]|30)\\.png",
            message = "must be one of the bundled preset avatars"
    )
    private String avatarUrl;
    private Boolean shareMilestones;
}
