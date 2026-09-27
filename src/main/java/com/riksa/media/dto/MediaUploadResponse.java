package com.riksa.media.dto;

/**
 * @param id  the stored image's name, used by {@code GET /api/v1/media/{id}}
 * @param url absolute URL to hand straight to {@code media} on a post
 */
public record MediaUploadResponse(String id, String url, Integer width, Integer height) {}
