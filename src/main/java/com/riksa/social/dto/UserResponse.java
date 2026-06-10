package com.riksa.social.dto;

import java.util.UUID;

public record UserResponse (
    UUID id,
    String username,
    String displayName,
    String avatartUrl
){}
