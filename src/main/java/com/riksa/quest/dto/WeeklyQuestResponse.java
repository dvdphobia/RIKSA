package com.riksa.quest.dto;

import java.util.UUID;

public record WeeklyQuestResponse(UUID id, String type, String title, String titleKm, int target,
        int progress, int xpReward, boolean completed, boolean claimed) {}
