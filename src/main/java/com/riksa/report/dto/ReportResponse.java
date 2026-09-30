package com.riksa.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ReportResponse {
    private UUID id;
    private UUID reporterId;
    private String targetType;
    private UUID targetId;
    private String reason;
    private String details;
    private String status;
    private UUID resolvedBy;
    private OffsetDateTime resolvedAt;
    private OffsetDateTime createdAt;
}
