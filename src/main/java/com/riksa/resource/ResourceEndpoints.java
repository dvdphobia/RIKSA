package com.riksa.resource;

public final class ResourceEndpoints {
    private ResourceEndpoints() {}

    public static final String LESSON_RESOURCES = "/api/v1/learning/lessons/{lessonId}/resources";
    public static final String RESOURCE_BY_ID = "/api/v1/resources/{resourceId}";
    public static final String RESOURCE_SAVE = "/api/v1/resources/{resourceId}/save";
}
