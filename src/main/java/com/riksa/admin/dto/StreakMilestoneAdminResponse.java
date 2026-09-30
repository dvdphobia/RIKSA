package com.riksa.admin.dto;

import java.util.UUID;

public record StreakMilestoneAdminResponse(UUID id, long minStreak, String streakName, String iconUrl) {
}
