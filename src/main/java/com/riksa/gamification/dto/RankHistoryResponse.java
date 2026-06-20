package com.riksa.gamification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RankHistoryResponse {
    private UUID id;
    private String scope;
    private String oldRank;
    private String newRank;
    private OffsetDateTime changedAt;
}
