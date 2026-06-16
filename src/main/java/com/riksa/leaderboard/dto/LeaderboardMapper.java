package com.riksa.leaderboard.dto;

import com.riksa.leaderboard.entity.LeaderboardSnapshot;

public class LeaderboardMapper {
    public static LeaderboardRowResponse toRow(LeaderboardSnapshot s){
        return new LeaderboardRowResponse(
                s.getRank(),
                s.getUserId(),
                s.getScore(),
                s.getScope(),
                s.getScopeId(),
                s.getRankingTier(),
                s.getSnapshotDate()
        );
    }
}
