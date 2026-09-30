package com.riksa.admin.dto;

public record LeaderboardRefreshResponse(int countryRows, int provinceRows, boolean redisRebuilt) {}
