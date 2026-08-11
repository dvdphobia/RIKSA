package com.riksa.admin.dto;

import com.riksa.auth.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateRequest {
    @NotNull
    private Role role;
}
