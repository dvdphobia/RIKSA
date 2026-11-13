package com.riksa.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
public class CategoryResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String iconUrl;
    private String backgroundKey;
    private String backgroundImageUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}