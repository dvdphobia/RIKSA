package com.riksa.social.dto;

import java.util.UUID;

public record UpdateCommentInput(
        UUID id,
        String content
) {}