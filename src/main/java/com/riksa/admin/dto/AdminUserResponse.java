package com.riksa.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String username,
        String displayName,
        String email,
        Boolean emailVerified,
        String avatarUrl,
        String role,
        OffsetDateTime suspendedUntil,
        String suspensionReason,
        OffsetDateTime lockedUntil,
        OffsetDateTime createdAt
) {}
