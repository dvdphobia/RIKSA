package com.riksa.social.dto;

import java.util.UUID;

public record UpdatePostInput(
        UUID id,
        String title,
        String content
) {}