package com.riksa.gamification.listener;

import com.riksa.gamification.event.XpGainedEvent;
import com.riksa.gamification.service.RankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for XP_GAINED events and checks whether the user's rank
 * should change. If so, inserts a RankHistory row and publishes
 * a RANK_CHANGED event.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankListener {

    private final RankService rankService;

    @EventListener
    public void handleXpGained(XpGainedEvent event) {
        log.info("Checking rank for user {} with new total XP: {}", event.getUserId(), event.getNewTotalXp());

        try {
            rankService.checkAndPromoteRank(event.getUserId(), event.getNewTotalXp());
        } catch (Exception e) {
            log.error("Failed to check/promote rank for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }
}