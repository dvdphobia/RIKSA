package com.riksa.auth.dto;

import com.riksa.common.enums.UserRole;
import lombok.*;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String nameKhmer;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private Long schoolId;
    private Long provinceId;
    private UserRole role;
    private String grade;
    private Boolean isPhoneVerified;
    private Boolean isEmailVerified;
    private OffsetDateTime createdAt;
}
