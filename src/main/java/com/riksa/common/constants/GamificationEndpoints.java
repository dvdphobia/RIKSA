package com.riksa.common.constants;

/**
 * Gamification module endpoint paths.
 * Base path: /api/v1/gamification
 */
public final class GamificationEndpoints {

    private GamificationEndpoints() {}

    public static final String BASE = ApiConstants.API_V1 + "/gamification";

    public static final String ME = "/me";
    public static final String XP = "/xp";
    public static final String ACHIEVEMENTS = "/achievements";
    public static final String RANK_HISTORY = "/rank-history";
    public static final String STREAK = "/streak";
    public static final String LEADERBOARD = "/leaderboard";
}