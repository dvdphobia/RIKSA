package com.riksa.admin.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AppFlagsSettings(
        @NotNull Boolean maintenanceMode,
        @NotNull String maintenanceMessage,
        @NotBlank @Pattern(regexp = "\\d+\\.\\d+\\.\\d+") String minAppVersion,
        @NotBlank @Pattern(regexp = "\\d+\\.\\d+\\.\\d+") String latestAppVersion) {
}
