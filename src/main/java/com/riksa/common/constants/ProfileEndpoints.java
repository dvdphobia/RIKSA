package com.riksa.common.constants;

/**
 * Public profile endpoint paths.
 */
public final class ProfileEndpoints {

    private ProfileEndpoints() {}

    public static final String BASE = ApiConstants.API_V1 + "/profiles";
    public static final String BY_USER_ID = "/{userId}";
}
