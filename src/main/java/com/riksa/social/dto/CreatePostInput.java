package com.riksa.social.dto;

import java.util.UUID;
import java.util.List;

public record CreatePostInput(
        String title,
        String content,
        String mediaUrl,
        List<PostMediaInput> media,
        UUID categoryId
) {}
