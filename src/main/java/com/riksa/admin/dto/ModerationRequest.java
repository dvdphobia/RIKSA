package com.riksa.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModerationRequest {
    @NotNull
    private String status; // "APPROVED" or "REJECTED"

    private String reason;
}