package com.riksa.buddy.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record BuddyResponse(
        String species,
        String name,
        boolean chosen,
        int energy,
        int bond,
        int bondLevel,
        OffsetDateTime updatedAt,
        List<BuddyTodayActionResponse> today,
        Map<String, BuddyEquippedItem> equipped,
        long balance) {}
