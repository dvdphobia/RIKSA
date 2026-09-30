package com.riksa.social.dto;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String iconUrl,
        int memberCount,
        boolean joined,
        boolean official,
        boolean ownedByMe,
        String postPermission,
        UUID pinnedPostId,
        String bannerUrl,
        String bannerColor
) {}