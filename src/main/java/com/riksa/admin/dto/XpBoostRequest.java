package com.riksa.admin.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record XpBoostRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("1.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 2) BigDecimal multiplier,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt) {
}
