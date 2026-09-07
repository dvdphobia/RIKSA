package com.riksa.leaderboard.dto;

import com.riksa.common.enums.ScopeType;
import com.riksa.common.enums.TierType;

import java.time.LocalDate;
import java.util.UUID;

public record LeaderboardRowResponse (
    int rank,
    UUID userId,
    int score,
    ScopeType scope,
    String scopeId,
    TierType rankingTier,
    LocalDate snapshotDate,
    String displayName,
    String avatarUrl
){}
