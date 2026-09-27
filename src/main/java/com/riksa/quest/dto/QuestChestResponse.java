package com.riksa.quest.dto;

import java.util.UUID;

public record QuestChestResponse(UUID id, int points, int target, int maxPoints,
        boolean opened, int xpReward, Object rewardItem) {}
