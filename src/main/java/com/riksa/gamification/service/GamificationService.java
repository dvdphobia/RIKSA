package com.riksa.gamification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.gamification.dto.AchievementResponse;
import com.riksa.gamification.dto.CursorPage;
import com.riksa.gamification.dto.GamificationProfileResponse;
import com.riksa.gamification.dto.LeaderboardEntryResponse;
import com.riksa.gamification.dto.RankHistoryResponse;
import com.riksa.gamification.dto.StreakResponse;
import com.riksa.gamification.dto.XpTransactionResponse;
import com.riksa.gamification.entity.Achievement;
import com.riksa.gamification.entity.RankHistory;
import com.riksa.gamification.entity.RankTier;
import com.riksa.gamification.entity.StreakMilestone;
import com.riksa.gamification.entity.UserAchievement;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.entity.XpTransaction;
import com.riksa.gamification.repository.AchievementRepository;
import com.riksa.gamification.repository.RankHistoryRepository;
import com.riksa.gamification.repository.RankTierRepository;
import com.riksa.gamification.repository.StreakMilestoneRepository;
import com.riksa.gamification.repository.UserAchievementRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.repository.XpTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GamificationService {
    private final UserGamificationRepository userGamificationRepository;
    private final RankTierRepository rankTierRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final RankHistoryRepository rankHistoryRepository;
    private final StreakMilestoneRepository streakMilestoneRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ── cursor records (used internally for encode/decode) ──

    private record XpCursor(OffsetDateTime t, UUID i) {}
    private record LeaderboardCursor(int x, UUID u, int r) {}

    // ── helpers ──

    private String encodeCursor(Object cursor) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(cursor));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to encode cursor", e);
        }
    }

    private <T> T decodeCursor(String cursor, Class<T> type) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cursor);
            return objectMapper.readValue(bytes, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor", e);
        }
    }

    // ── profile ──

    @Transactional(readOnly = true)
    public GamificationProfileResponse getProfile(UUID userId) {
        UserGamification user = userGamificationRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User gamification not found"));

        RankTier currentTier = rankTierRepository
                .findFirstByMinXpLessThanEqualOrderByMinXpDesc(user.getTotalXp())
                .orElse(null);

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

    // ── xp history (cursor-based, no COUNT query) ──

    @Transactional(readOnly = true)
    public CursorPage<XpTransactionResponse> getXpHistory(UUID userId, String cursorStr, int limit) {
        limit = Math.min(Math.max(limit, 1), 50);
        XpCursor cursor = decodeCursor(cursorStr, XpCursor.class);

        OffsetDateTime cursorCreatedAt = cursor != null ? cursor.t() : null;
        UUID cursorId = cursor != null ? cursor.i() : null;

        List<XpTransaction> rows = xpTransactionRepository.findByUserIdAfterCursor(
                userId, cursorCreatedAt, cursorId, PageRequest.of(0, limit + 1));

        List<XpTransactionResponse> edges = rows.stream()
                .limit(limit)
                .map(this::toXpResponse)
                .toList();

        boolean hasNext = rows.size() > limit;
        String endCursor = null;
        if (hasNext) {
            XpTransaction last = rows.get(limit);
            endCursor = encodeCursor(new XpCursor(last.getCreatedAt(), last.getId()));
        }

        return new CursorPage<>(edges, hasNext, endCursor);
    }

    private XpTransactionResponse toXpResponse(XpTransaction tx) {
        XpTransactionResponse r = new XpTransactionResponse();
        r.setId(tx.getId());
        r.setAmount(tx.getAmount());
        r.setSourceType(tx.getSourceType());
        r.setSourceId(tx.getSourceId());
        r.setRunningBalance(tx.getRunningBalance());
        r.setCreatedAt(tx.getCreatedAt());
        return r;
    }

    // ── achievements ──

    @Transactional(readOnly = true)
    public List<AchievementResponse> getAchievements(UUID userId) {
        List<Achievement> all = achievementRepository.findAll();
        List<UserAchievement> earned = userAchievementRepository.findByUserId(userId);
        Set<UUID> earnedIds = earned.stream()
                .map(UserAchievement::getAchievementId)
                .collect(Collectors.toSet());
        Map<UUID, OffsetDateTime> unlockedAtMap = earned.stream()
                .collect(Collectors.toMap(
                        UserAchievement::getAchievementId,
                        UserAchievement::getUnlockedAt));

        return all.stream().map(a -> {
            AchievementResponse r = new AchievementResponse();
            r.setId(a.getId());
            r.setName(a.getName());
            r.setDescription(a.getDescription());
            r.setIconUrl(a.getIconUrl());
            r.setEarned(earnedIds.contains(a.getId()));
            r.setUnlockedAt(unlockedAtMap.get(a.getId()));
            return r;
        }).toList();
    }

    // ── rank history ──

    @Transactional(readOnly = true)
    public List<RankHistoryResponse> getRankHistory(UUID userId) {
        List<RankHistory> history = rankHistoryRepository.findByUserIdOrderByChangedAtDesc(userId);
        return history.stream().map(h -> {
            RankHistoryResponse r = new RankHistoryResponse();
            r.setId(h.getId());
            r.setScope(h.getScope().name());
            r.setOldRank(h.getOldRank().name());
            r.setNewRank(h.getNewRank().name());
            r.setChangedAt(h.getChangedAt());
            return r;
        }).toList();
    }

    // ── streak ──

    @Transactional(readOnly = true)
    public StreakResponse getStreak(UUID userId) {
        UserGamification user = userGamificationRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User gamification not found"));

        StreakMilestone currentMilestone = streakMilestoneRepository
                .findFirstByMinStreakLessThanEqualOrderByMinStreakDesc(user.getCurrentStreak())
                .orElse(null);

        List<StreakMilestone> all = streakMilestoneRepository.findAll();
        StreakMilestone nextMilestone = all.stream()
                .filter(m -> m.getMinStreak() > user.getCurrentStreak())
                .findFirst()
                .orElse(null);

        StreakResponse r = new StreakResponse();
        r.setCurrentStreak(user.getCurrentStreak());
        r.setHighestStreak(user.getHighestStreak());
        r.setCurrentMilestoneName(currentMilestone != null ? currentMilestone.getStreakName() : null);
        r.setCurrentMilestoneIcon(currentMilestone != null ? currentMilestone.getIconUrl() : null);
        r.setNextMilestoneName(nextMilestone != null ? nextMilestone.getStreakName() : null);
        r.setNextMilestoneMinStreak(nextMilestone != null ? nextMilestone.getMinStreak() : 0);
        r.setDaysToNext(nextMilestone != null ? nextMilestone.getMinStreak() - user.getCurrentStreak() : 0);

        return r;
    }

    // ── leaderboard ──

    @Transactional(readOnly = true)
    public CursorPage<LeaderboardEntryResponse> getLeaderboard(String cursorStr, int limit) {
        limit = Math.min(Math.max(limit, 1), 50);
        LeaderboardCursor cursor = decodeCursor(cursorStr, LeaderboardCursor.class);

        Integer cursorXp = cursor != null ? cursor.x() : null;
        UUID cursorUserId = cursor != null ? cursor.u() : null;
        int startRank = cursor != null ? cursor.r() : 0;

        List<UserGamification> rows = userGamificationRepository.findLeaderboardAfterCursor(
                cursorXp, cursorUserId, PageRequest.of(0, limit + 1));

        List<UUID> userIds = rows.stream().map(UserGamification::getUserId).toList();
        Map<UUID, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        List<RankTier> allTiers = rankTierRepository.findAll();

        List<LeaderboardEntryResponse> edges = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(rows.size(), limit); i++) {
            edges.add(toLeaderboardEntry(rows.get(i), usersById, allTiers, startRank + i));
        }

        boolean hasNext = rows.size() > limit;
        String endCursor = null;
        if (hasNext) {
            UserGamification last = rows.get(limit);
            int lastRank = startRank + limit;
            endCursor = encodeCursor(new LeaderboardCursor(last.getTotalXp(), last.getUserId(), lastRank));
        }

        return new CursorPage<>(edges, hasNext, endCursor);
    }

    private LeaderboardEntryResponse toLeaderboardEntry(UserGamification ug, Map<UUID, User> usersById,
                                                         List<RankTier> allTiers, int rank) {
        User user = usersById.get(ug.getUserId());
        RankTier tier = allTiers.stream()
                .filter(t -> t.getMinXp() <= ug.getTotalXp())
                .sorted((a, b) -> Integer.compare(b.getMinXp(), a.getMinXp()))
                .findFirst()
                .orElse(null);

        LeaderboardEntryResponse e = new LeaderboardEntryResponse();
        e.setRank(rank + 1);
        e.setUserId(ug.getUserId());
        e.setDisplayName(user != null ? user.getDisplayName() : "Unknown");
        e.setAvatarUrl(user != null ? user.getAvatarUrl() : null);
        e.setTotalXp(ug.getTotalXp());
        e.setTierName(tier != null ? tier.getTierName().name() : null);
        e.setTierIcon(tier != null ? tier.getIconUrl() : null);
        return e;
    }
}
