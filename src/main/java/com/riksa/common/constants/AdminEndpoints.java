package com.riksa.common.constants;

/**
 * Admin module endpoint paths.
 * Base path: /api/v1/admin
 */
public final class AdminEndpoints {

    private AdminEndpoints() {}

    public static final String BASE = ApiConstants.API_V1 + "/admin";

    // --- Categories ---
    public static final String CATEGORIES = "/categories";
    public static final String ROOT = "";
    public static final String CATEGORY_BY_ID = "/{id}";

    // --- Users ---
    public static final String USERS = "/users";
    public static final String USER_ROLE = "/{id}/role";
    public static final String USER_SUSPENSION = "/{id}/suspension";

    // --- Blog moderation ---
    public static final String BLOG = "/blog";
    public static final String BLOG_STATUS = "/{id}/status";

    // --- Audit logs ---
    public static final String AUDIT_LOGS = "/audit-logs";

    // --- Stats ---
    public static final String STATS = "/stats";
    public static final String STATS_OVERVIEW = "/overview";
    public static final String STATS_EXERCISES = "/exercises";
    public static final String STATS_GAMIFICATION = "/gamification";
    public static final String STATS_SECURITY = "/security";

    // --- Reports ---
    public static final String REPORTS = "/reports";
    public static final String REPORT_BY_ID = "/{id}";

    // --- Social moderation ---
    public static final String POSTS = "/posts";
    public static final String POST_STATUS = "/{id}/status";
    public static final String COMMENTS = "/comments";
    public static final String COMMENT_STATUS = "/{id}/status";

    // --- Lessons ---
    public static final String LESSONS = "/lessons";
    public static final String LESSON_BY_ID = "/{id}";
    public static final String LESSON_BULK = "/bulk";
    public static final String LESSON_UPLOAD = "/upload";
    public static final String LESSON_UPLOAD_BULK = "/upload/bulk";

    // ── Roadmap and media moderation ──
    public static final String ROADMAP_UNITS = "/roadmap-units";
    public static final String ROADMAP_UNIT_BY_ID = "/{id}";
    public static final String ROADMAP_UNIT_REORDER = "/reorder";
    public static final String LESSON_REORDER = "/reorder";
    public static final String POST_MEDIA = "/{id}/media";
    public static final String COMMENT_MEDIA = "/{id}/media";

    // ── Gamification administration and platform settings ──
    public static final String ADMIN_GAMIFICATION = "/gamification";
    public static final String ADMIN_ACHIEVEMENTS = "/achievements";
    public static final String ADMIN_RANK_TIERS = "/rank-tiers";
    public static final String ADMIN_STREAK_MILESTONES = "/streak-milestones";
    public static final String ADMIN_XP_BOOSTS = "/xp-boosts";
    public static final String ADMIN_XP_AUDIT = "/xp-audit";
    public static final String ADMIN_SETTINGS = "/settings";

    // ── User detail, leaderboard, and AI chat ──
    public static final String USER_OVERVIEW = "/{id}/overview";
    public static final String USER_XP = "/{id}/xp";
    public static final String USER_STREAK = "/{id}/streak";
    public static final String USER_ACHIEVEMENTS = "/{id}/achievements/{achievementId}";
    public static final String USER_LOCK = "/{id}/lock";
    public static final String USER_LEADERBOARD_EXCLUSION = "/{id}/leaderboard-exclusion";

    public static final String ADMIN_LEADERBOARD = "/leaderboard";
    public static final String ADMIN_LEADERBOARD_META = "/meta";
    public static final String ADMIN_LEADERBOARD_REFRESH = "/refresh";

    public static final String ADMIN_AI_CHAT = "/ai-chat";
    public static final String ADMIN_AI_CHAT_USAGE = "/usage";
    public static final String ADMIN_AI_CHAT_USER_SESSIONS = "/users/{userId}/sessions";
    public static final String ADMIN_AI_CHAT_SESSION = "/sessions/{id}";

    // ── Buddy market and lesson resources ──
    public static final String ADMIN_MARKET_ITEMS = "/market/items";
    public static final String ADMIN_MARKET_ITEM_BY_ID = "/{id}";
    public static final String ADMIN_RESOURCES = "/resources";
    public static final String ADMIN_RESOURCE_BY_ID = "/{id}";
    public static final String ADMIN_RESOURCE_STATUS = "/{id}/status";
}
