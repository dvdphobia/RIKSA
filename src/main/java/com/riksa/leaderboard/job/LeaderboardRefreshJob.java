package com.riksa.leaderboard.job;

import com.riksa.common.event.EventPublisher;
import com.riksa.leaderboard.event.LeaderboardRefreshedEvent;
import com.riksa.leaderboard.service.LeaderboardSnapshotService;
import com.riksa.leaderboard.service.RedisLeaderboardStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardRefreshJob {
    private final LeaderboardSnapshotService snapshotService;
    private final EventPublisher eventPublisher;
    private final RedisLeaderboardStore redisLeaderboardStore;

    @Scheduled(cron = "0 */5 * * * *", zone = "UTC")
    @SchedulerLock(name = "leaderboard-refresh", lockAtMostFor = "10m", lockAtLeastFor = "4m")
    public void refresh() {
        log.info("LeaderboardRefreshJob triggered");
        snapshotService.refreshSnapshots();

        try {
            redisLeaderboardStore.replaceAll(snapshotService.loadSnapshots(LocalDate.now()));
        } catch (RuntimeException e) {
            log.error("PostgreSQL leaderboard refresh succeeded, but Redis rebuild failed", e);
        }

        eventPublisher.publish(new LeaderboardRefreshedEvent(
                this, LocalDate.now(), -1));
    }
}
