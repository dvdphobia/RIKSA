package com.riksa.social.dto;

import com.riksa.social.enums.MilestoneType;

public record MilestoneResponse(
        MilestoneType type,
        String title,
        String titleKm,
        Integer value
) {}
