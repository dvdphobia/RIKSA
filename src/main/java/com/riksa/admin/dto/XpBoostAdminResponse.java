package com.riksa.admin.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record XpBoostAdminResponse(
        UUID id,
        String name,
        BigDecimal multiplier,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String status,
        UUID createdBy) {
}
