package com.riksa.buddy.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record BuddyResponse(
        String species,
        String name,
        int energy,
        int bond,
        int bondLevel,
        OffsetDateTime updatedAt,
        List<BuddyTodayActionResponse> today) {}
