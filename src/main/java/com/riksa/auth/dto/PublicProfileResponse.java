package com.riksa.auth.dto;

import com.riksa.gamification.dto.GamificationProfileResponse;
import com.riksa.buddy.dto.BuddyLookResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Intentionally public subset of a user's profile. Private account and
 * verification fields must not be added to this response.
 */
public record PublicProfileResponse(
        UUID userId,
        String username,
        String displayName,
        String avatarUrl,
        String bio,
        String role,
        String countryId,
        ProvinceResponse province,
        OffsetDateTime joinedAt,
        GamificationProfileResponse gamification,
        BuddyLookResponse buddy
) {}
