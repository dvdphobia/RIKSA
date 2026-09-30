package com.riksa.social.dto;

public record CreateCommunityInput(
        String name,
        String description,
        String iconUrl
) {}
