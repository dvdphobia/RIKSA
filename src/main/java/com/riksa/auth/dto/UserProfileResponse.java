package com.riksa.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String username;
    private String displayName;
    private String email;
    private Boolean emailVerified;
    private String avatarUrl;
    private String bio;
    private String gender;
    private String phoneNumber;
    private Boolean phoneVerified;
    private String role;
    private String countryId;
    private UUID provinceId;
    private OffsetDateTime createdAt;
    private Boolean shareMilestones;
}
