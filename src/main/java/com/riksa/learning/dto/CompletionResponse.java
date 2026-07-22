package com.riksa.learning.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CompletionResponse {
    private UUID completionId;
    private UUID lessonId;
    private int xpEarned;
    private int score;
    private boolean passed;
    private OffsetDateTime completedAt;
}
