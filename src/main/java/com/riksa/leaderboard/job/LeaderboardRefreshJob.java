package com.riksa.leaderboard.job;

import com.riksa.common.event.EventPublisher;
import com.riksa.leaderboard.event.LeaderboardRefreshedEvent;
import com.riksa.leaderboard.service.LeaderboardSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardRefreshJob {
    private final LeaderboardSnapshotService snapshotService;
    private final EventPublisher eventPublisher;

    @Scheduled(cron = "0 */5 * * * *", zone = "UTC")
    public void refresh() {
        log.info("LeaderboardRefreshJob triggered");
        snapshotService.refreshSnapshots();

        eventPublisher.publish(new LeaderboardRefreshedEvent(
                this, LocalDate.now(), -1));
    }
}
