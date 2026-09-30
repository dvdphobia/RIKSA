package com.riksa.common.constants;

/**
 * Base API constants shared across all modules.
 */
public final class ApiConstants {

    private ApiConstants() {}

    public static final String API_V1 = "/api/v1";

    // --- Security role constants ---
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_MODERATOR = "ROLE_MODERATOR";
    public static final String ROLE_USER = "ROLE_USER";

    // --- Header constants ---
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
}