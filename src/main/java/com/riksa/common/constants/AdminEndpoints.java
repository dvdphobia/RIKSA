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
}