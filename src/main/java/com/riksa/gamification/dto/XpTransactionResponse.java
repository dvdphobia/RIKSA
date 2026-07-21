package com.riksa.gamification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class XpTransactionResponse {
    private UUID id;
    private long amount;
    private String sourceType;
    private UUID sourceId;
    private long runningBalance;
    private OffsetDateTime createdAt;
}