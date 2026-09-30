package com.riksa.report.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateReportRequest {
    @NotNull
    private String targetType; // "POST", "COMMENT" or "BLOG"

    @NotNull
    private UUID targetId;

    @NotNull
    private String reason; // "SPAM", "HARASSMENT", "NSFW", "MISINFORMATION" or "OTHER"

    @Size(max = 1000)
    private String details;
}
