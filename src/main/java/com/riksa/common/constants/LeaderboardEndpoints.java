package com.riksa.common.constants;

/**
 * Leaderboard module endpoint paths.
 * Base path: /api/v1/leaderboard
 */
public final class LeaderboardEndpoints {

    private LeaderboardEndpoints() {}

    public static final String BASE = ApiConstants.API_V1 + "/leaderboard";

    /** Root endpoint — empty string appended to BASE. */
    public static final String ROOT = "";
    public static final String ME = "/me";
}