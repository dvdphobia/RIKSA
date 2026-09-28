package com.riksa.admin.dto;

import com.riksa.common.enums.ScopeType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LeaderboardBoardResponse(
        LocalDate date,
        ScopeType scope,
        String scopeId,
        long total,
        int page,
        int size,
        List<Row> rows
) {
    public record Row(
            int rank,
            UUID userId,
            String username,
            String displayName,
            int score,
            String tier
    ) {}
}
