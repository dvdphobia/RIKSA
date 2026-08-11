package com.riksa.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OverviewStatsResponse {
    private long signups24h;
    private long signups7d;
    private long posts24h;
    private long posts7d;
    private long comments24h;
    private long comments7d;
    private long lessonCompletions24h;
    private long lessonCompletions7d;
    private long pendingBlogs;
    private long openReports;
}
