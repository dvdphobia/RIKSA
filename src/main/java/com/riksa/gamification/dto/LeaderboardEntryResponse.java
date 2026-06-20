package com.riksa.gamification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class LeaderboardEntryResponse {
    private int rank;
    private UUID userId;
    private String displayName;
    private String avatarUrl;
    private long totalXp;
    private String tierName;
    private String tierIcon;
}