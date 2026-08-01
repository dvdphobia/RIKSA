package com.riksa.common.constants;

/**
 * Auth module endpoint paths.
 * Base path: /api/v1/auth
 */
public final class AuthEndpoints {

    private AuthEndpoints() {}

    public static final String BASE = ApiConstants.API_V1 + "/auth";

    public static final String REGISTER = "/register";
    public static final String LOGIN = "/login";
    public static final String REFRESH = "/refresh";
    public static final String LOGOUT = "/logout";
    public static final String GOOGLE = "/google";
    public static final String OTP_SEND = "/otp/send";
    public static final String OTP_VERIFY = "/otp/verify";
    public static final String ME = "/me";
    public static final String FORGOT_PASSWORD = "/forgot-password";
    public static final String RESET_PASSWORD = "/reset-password";
    public static final String VERIFY_EMAIL = "/verify-email";
}