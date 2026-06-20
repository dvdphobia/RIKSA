package com.riksa.leaderboard.job;

import com.riksa.leaderboard.service.LeaderboardQueryService;
import com.riksa.leaderboard.service.LeaderboardSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardRefreshJob {
    private final LeaderboardSnapshotService snapshotService;

    @Scheduled(cron = "0 */5 * * * *", zone = "UTC")
    public void refresh() {
        log.info("LeaderboardRefreshJob triggered");
        snapshotService.refreshSnapshots();
    }
}
