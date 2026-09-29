package com.riksa.common.constants;

public final class NotificationEndpoints {

    private NotificationEndpoints() {}

    public static final String BASE = ApiConstants.API_V1 + "/notifications";
    public static final String UNREAD_COUNT = "/unread-count";
    public static final String READ = "/{id}/read";
    public static final String READ_ALL = "/read-all";
}
