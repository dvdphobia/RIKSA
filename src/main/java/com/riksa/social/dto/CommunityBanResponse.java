package com.riksa.social.dto;

import java.time.OffsetDateTime;

public record CommunityBanResponse(
        UserResponse user,
        String reason,
        OffsetDateTime createdAt
) {}
