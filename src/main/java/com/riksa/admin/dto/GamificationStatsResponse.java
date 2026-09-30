package com.riksa.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class GamificationStatsResponse {
    private List<DailyXp> xpByDay;
    private List<StreakBucket> streakHistogram;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class DailyXp {
        private LocalDate date;
        private String sourceType;
        private long totalXp;
        private long transactionCount;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class StreakBucket {
        private String bucket;
        private long userCount;
    }
}
