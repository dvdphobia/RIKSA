package com.riksa.common.constants;

/**
 * Media module endpoint paths.
 * Base path: /api/v1/media
 */
public final class MediaEndpoints {

    private MediaEndpoints() {}

    public static final String BASE = ApiConstants.API_V1 + "/media";

    /** Root endpoint — upload an image. */
    public static final String ROOT = "";

    /** Serves a stored image by its generated id. */
    public static final String ITEM = "/{id}";
}
