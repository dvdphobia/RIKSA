package com.riksa.social.dto;

import java.time.OffsetDateTime;

public record CommunityMemberResponse(
        UserResponse user,
        String role,
        OffsetDateTime joinedAt
) {}
