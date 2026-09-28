package com.riksa.moderation;

/**
 * Content kind supplied to moderation. The current rules assess title and body
 * together for every surface; callers may use the surface later to select
 * different policy without changing the input contract.
 */
public enum ModerationSurface {
    BLOG,
    POST,
    COMMENT
}
