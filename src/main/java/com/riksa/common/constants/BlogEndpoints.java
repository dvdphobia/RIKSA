package com.riksa.common.constants;

/**
 * Blog module endpoint paths.
 * Base path: /api/v1/blog
 */
public final class BlogEndpoints {

    private BlogEndpoints() {}

    public static final String BASE = ApiConstants.API_V1 + "/blog";

    /** Root endpoint — list / create posts. */
    public static final String ROOT = "";
    public static final String MY_POSTS = "/my-posts";
    public static final String POST = "/{id}";
    public static final String UPDATE = "/{id}";
    public static final String DELETE = "/{id}";
    public static final String LIKE = "/{id}/like";
    public static final String VIEW = "/{id}/view";
    public static final String READ_COMPLETE = "/{id}/read-complete";
    public static final String ADMIN_STATUS = "/admin/{id}/status";
}
