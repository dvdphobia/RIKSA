package com.riksa.resource.dto;

import java.util.UUID;

public record ResourceAuthorResponse(
        UUID id,
        String displayName,
        String avatarUrl
) {}
