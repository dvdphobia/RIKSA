package com.riksa.admin.dto;

import com.riksa.common.enums.ScopeType;

import java.time.LocalDate;
import java.util.List;

public record LeaderboardMetaResponse(
        List<LocalDate> dates,
        List<ScopeMeta> scopes
) {
    public record ScopeMeta(ScopeType scope, String scopeId, String label, long users) {}
}
