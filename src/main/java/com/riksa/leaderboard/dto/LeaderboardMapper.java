package com.riksa.leaderboard.dto;

import com.riksa.auth.entity.User;
import com.riksa.leaderboard.entity.LeaderboardSnapshot;

public class LeaderboardMapper {

    /**
     * Maps a snapshot to its API row, enriching it with the batch-loaded
     * user profile so clients can render displayName/avatarUrl without
     * issuing N+1 follow-up requests.
     */
    public static LeaderboardRowResponse toRow(LeaderboardSnapshot s, User user) {
        return new LeaderboardRowResponse(
                s.getRank(),
                s.getUserId(),
                s.getScore(),
                s.getScope(),
                s.getScopeId(),
                s.getRankingTier(),
                s.getSnapshotDate(),
                user != null ? user.getDisplayName() : null,
                user != null ? user.getAvatarUrl() : null
        );
    }
}
