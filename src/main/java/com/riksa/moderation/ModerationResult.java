package com.riksa.moderation;

import java.util.List;

/**
 * Immutable moderation outcome. The score is the sum of mild term weights and
 * spam-signal points; severe word-list terms block regardless of score.
 */
public record ModerationResult(ModerationVerdict verdict, int score, List<ModerationReason> reasons) {
    public ModerationResult {
        reasons = List.copyOf(reasons);
    }
}
