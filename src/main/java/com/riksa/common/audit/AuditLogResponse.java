package com.riksa.common.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class AuditLogResponse {
    private UUID id;
    private UUID actorId;
    private String action;
    private String targetType;
    private UUID targetId;
    private Map<String, Object> details;
    private OffsetDateTime createdAt;
}
