package com.riksa.admin.dto;

import java.util.UUID;

public record RankTierAdminResponse(UUID id, String tierName, long minXp, String iconUrl, long userCount) {
}
