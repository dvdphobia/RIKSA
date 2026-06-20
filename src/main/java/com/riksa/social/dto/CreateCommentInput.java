package com.riksa.social.dto;

import java.util.UUID;

public record CreateCommentInput(
        UUID postId,
        String content,
        UUID parentId
) {}