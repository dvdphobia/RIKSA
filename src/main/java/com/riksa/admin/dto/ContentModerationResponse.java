package com.riksa.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ContentModerationResponse {
    private UUID id;
    private String status;
    private String reason;
    private UUID hiddenBy;
    private OffsetDateTime hiddenAt;
}
