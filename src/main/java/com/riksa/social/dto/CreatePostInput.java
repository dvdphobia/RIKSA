package com.riksa.social.dto;

import java.util.UUID;

public record CreatePostInput(
        String title,
        String content,
        String mediaUrl,
        UUID categoryId
) {}