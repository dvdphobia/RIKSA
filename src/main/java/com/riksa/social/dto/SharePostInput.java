package com.riksa.social.dto;

import com.riksa.social.enums.SharePlatform;

import java.util.UUID;

public record SharePostInput(
        UUID postId,
        SharePlatform platform
) {}