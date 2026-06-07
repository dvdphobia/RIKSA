package com.riksa.gamification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.riksa.gamification.dto.GamificationProfileResponse;
import com.riksa.gamification.entity.RankTier;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.repository.RankTierRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class GamificationService {
    private final UserGamificationRepository userGamificationRepository;
    private final RankTierRepository rankTierRepository;

    @Transactional(readOnly = true)
    public GamificationProfileResponse getProfile(UUID userId) {
        UserGamification user = userGamificationRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User gamification not found"));

        RankTier currentTier = rankTierRepository
                .findFirstByMinXpLessThanEqualOrderByMinXpDesc(user.getTotalXp())
                .orElse(null);

        // find next tier above current xp
        List<RankTier> allTiers = rankTierRepository.findAll();

        RankTier nextTier = allTiers.stream()
        .filter(t -> t.getMinXp() > user.getTotalXp())
        .findFirst()
        .orElse(null);


        GamificationProfileResponse response = new GamificationProfileResponse();
        response.setUserId(user.getUserId());
        response.setTotalXp(user.getTotalXp());
        response.setCurrentStreak(user.getCurrentStreak());
        response.setHighestStreak(user.getHighestStreak());
        response.setCurrentTier(currentTier != null ? currentTier.getTierName().name() : null);
        response.setCurrentTierIcon(currentTier != null ? currentTier.getIconUrl() : null);
        response.setNextTier(nextTier != null ? nextTier.getTierName().name() : null);
        response.setXpToNext(nextTier != null ? nextTier.getMinXp() - user.getTotalXp() : 0);

        return response;
    }
}
