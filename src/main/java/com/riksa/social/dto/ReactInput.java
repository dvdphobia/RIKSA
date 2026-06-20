package com.riksa.social.dto;

import com.riksa.social.enums.ReactionType;

import java.util.UUID;

public record ReactInput(
        UUID postId,
        UUID commentId,
        ReactionType type
) {}