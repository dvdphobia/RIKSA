package com.riksa.leaderboard.dto;

import com.riksa.common.enums.TierType;

public record MyRankResponse(
        int countryRank,
        int countryScore,
        TierType countryTier,
        Integer provinceRank,
        Integer provinceScore,
        TierType provinceTier
) {}