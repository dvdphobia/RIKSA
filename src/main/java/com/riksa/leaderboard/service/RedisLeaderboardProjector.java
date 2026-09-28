package com.riksa.leaderboard.service;

import com.riksa.auth.repository.UserRepository;
import com.riksa.gamification.repository.RankTierRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisLeaderboardProjector {

    private final UserGamificationRepository userGamificationRepository;
    private final UserRepository userRepository;
    private final RankTierRepository rankTierRepository;
    private final RedisLeaderboardStore leaderboardStore;

    @Transactional(readOnly = true)
    public void update(UUID userId) {
        var gamification = userGamificationRepository.findByUserId(userId).orElse(null);
        var user = userRepository.findById(userId).orElse(null);
        if (user == null || user.isLeaderboardExcluded()) {
            leaderboardStore.removeUser(userId);
            return;
        }
        if (gamification == null) return;

        var tier = rankTierRepository
                .findFirstByMinXpLessThanEqualOrderByMinXpDesc(gamification.getTotalXp())
                .orElse(null);
        if (tier == null) return;

        int score = Math.toIntExact(gamification.getTotalXp() + gamification.getCurrentStreak() * 5L);
        String provinceId = user.getProvince() == null ? null : user.getProvince().getId().toString();
        leaderboardStore.updateUser(userId, score, tier.getTierName(), provinceId);
    }
}
