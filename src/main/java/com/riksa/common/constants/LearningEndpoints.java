package com.riksa.common.constants;

/**
 * Learning module endpoint paths.
 * Base path: /api/v1/lessons
 */
public final class LearningEndpoints {

    private LearningEndpoints() {}

    public static final String BASE = ApiConstants.API_V1 + "/lessons";

    /** Root endpoint — list lessons. */
    public static final String ROOT = "";
    public static final String ROADMAP = "/roadmap";
    public static final String LESSON_BY_ID = "/{id}";
    public static final String EXERCISE_ATTEMPT = "/{id}/exercises/{exerciseId}/attempt";
    public static final String COMPLETE = "/{id}/complete";
    public static final String START = "/{id}/start";
    public static final String SKIP_ROADMAP_UNIT = "/roadmap/units/{unitId}/skip";
    public static final String RESUME_ROADMAP_UNIT = "/roadmap/units/{unitId}/resume";
}
