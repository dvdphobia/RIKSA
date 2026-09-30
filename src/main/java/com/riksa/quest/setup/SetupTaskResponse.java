package com.riksa.quest.setup;

public record SetupTaskResponse(
        String key,
        String title,
        String titleKm,
        int xpReward,
        boolean done,
        boolean claimed
) {}
