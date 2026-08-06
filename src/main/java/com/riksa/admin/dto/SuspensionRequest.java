package com.riksa.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class SuspensionRequest {
    @NotBlank
    private String reason;

    @NotNull
    private OffsetDateTime until;
}
