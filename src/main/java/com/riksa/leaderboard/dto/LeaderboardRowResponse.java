package com.riksa.leaderboard.dto;

import com.riksa.common.enums.ScopeType;
import com.riksa.common.enums.TierType;

import java.time.LocalDate;

public record LeaderboardRowResponse (
    int rank,
    java.util.UUID userId,
    int score,
    ScopeType scope,
    String scopeId,
    TierType rankingTier,
    LocalDate snapshoDate
){}
