package com.riksa.gamification.job;

import com.riksa.gamification.service.StreakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreakResetJob {

    private final StreakService streakService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Phnom_Penh")
    public void resetStaleStreaks() {
        int resetCount = streakService.resetStaleStreaks();
        log.info("Scheduled streak reset completed; reset {} users", resetCount);
    }
}
