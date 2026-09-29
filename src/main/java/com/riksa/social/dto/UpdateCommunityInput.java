package com.riksa.social.dto;

import java.util.List;
import java.util.UUID;

/** Null leaves a field unchanged; a non-null rules list replaces every rule. */
public record UpdateCommunityInput(
        UUID id,
        String name,
        String description,
        String iconUrl,
        Boolean clearIcon,
        String postPermission,
        List<CommunityRuleInput> rules
) {}
