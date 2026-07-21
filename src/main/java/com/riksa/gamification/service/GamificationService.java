package com.riksa.gamification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.repository.BlogLikeRepository;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.common.exception.InvalidCursorException;
import com.riksa.common.exception.ResourceNotFoundException;
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
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.social.repository.CategoryRepository;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    // ── repos for achievement progress computation ──
    private final LessonCompletionRepository lessonCompletionRepository;
    private final LessonRepository lessonRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final ReactionRepository reactionRepository;
    private final BlogPostRepository blogPostRepository;
    private final BlogLikeRepository blogLikeRepository;

    // ── cursor records ──

    private record XpCursor(OffsetDateTime t, UUID i) {}
    private record LeaderboardCursor(long x, UUID u, int r) {}
    private record RankHistoryCursor(OffsetDateTime t, UUID i) {}

    // ── helpers ──

    private String encodeCursor(Object cursor) {
        try {
            return java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(cursor));
        } catch (JsonProcessingException e) {
            throw new InvalidCursorException("Failed to encode cursor", e);
        }
    }

    private <T> T decodeCursor(String cursor, Class<T> type) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            byte[] bytes = java.util.Base64.getUrlDecoder().decode(cursor);
            return objectMapper.readValue(bytes, type);
        } catch (Exception e) {
            throw new InvalidCursorException("Invalid cursor: " + e.getMessage(), e);
        }
    }

    // ── profile ──

    @Transactional(readOnly = true)
    public GamificationProfileResponse getProfile(UUID userId) {
        UserGamification user = userGamificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Gamification profile not found for user " + userId));

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

    // ── xp history (cursor-based) ──

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

        // Fetch user gamification state for progress computation
        UserGamification userGamification = userGamificationRepository.findByUserId(userId).orElse(null);

        return all.stream().map(a -> {
            AchievementResponse r = new AchievementResponse();
            r.setId(a.getId());
            r.setName(a.getName());
            r.setDescription(a.getDescription());
            r.setIconUrl(a.getIconUrl());
            r.setEarned(earnedIds.contains(a.getId()));
            r.setUnlockedAt(unlockedAtMap.get(a.getId()));
            r.setRarity(a.getRarity());
            r.setXpReward(a.getXpReward());

            // Compute progress
            int[] progress = computeProgress(a, userId, userGamification);
            r.setProgress(progress[0]);
            r.setProgressMax(progress[1]);
            r.setProgressPercent(progress[1] > 0 ? (double) progress[0] / progress[1] : 0.0);

            // If earned, force progress to max
            if (r.isEarned()) {
                r.setProgress(r.getProgressMax());
                r.setProgressPercent(1.0);
            }

            return r;
        }).toList();
    }

    /**
     * Compute current progress toward an achievement based on its criteria type.
     * Returns [current, max]. For binary achievements, returns [0, 1] when not earned.
     */
    private int[] computeProgress(Achievement a, UUID userId, UserGamification userGamification) {
        long max = a.getCriteriaValue() != null ? a.getCriteriaValue() : 1;
        if (max <= 0) max = 1; // binary achievements

        return switch (a.getCriteriaType()) {
            case EARN_XP -> {
                long xp = userGamification != null ? userGamification.getTotalXp() : 0;
                yield new int[]{(int) Math.min(xp, max), (int) max};
            }
            case COMPLETE_N_COURSES, COMPLETE_COURSE -> {
                long count = lessonCompletionRepository.countByUserId(userId);
                yield new int[]{(int) Math.min(count, max), (int) max};
            }
            case MAINTAIN_STREAK -> {
                int streak = userGamification != null ? userGamification.getCurrentStreak() : 0;
                yield new int[]{Math.min(streak, (int) max), (int) max};
            }
            case REACH_RANK -> {
                // Progress is ordinal-based, but showing 0/1 for binary is simpler
                yield new int[]{0, 1};
            }
            case CREATE_POST -> {
                long count = postRepository.countByAuthorId(userId);
                yield new int[]{(int) Math.min(count, max), (int) max};
            }
            case RECEIVE_REACTION -> {
                long count = reactionRepository.countReactionsOnUserPosts(userId);
                yield new int[]{(int) Math.min(count, max), (int) max};
            }
            case WRITE_BLOG_POST -> {
                long count = blogPostRepository.countByAuthorIdAndStatus(userId, BlogStatus.APPROVED);
                yield new int[]{(int) Math.min(count, max), (int) max};
            }
            case RECEIVE_BLOG_LIKE -> {
                long count = blogLikeRepository.countLikesOnUserBlogPosts(userId);
                yield new int[]{(int) Math.min(count, max), (int) max};
            }
            case COMPLETE_COURSE_WITH_DIFFICULTY -> {
                // Binary: completed a lesson with matching difficulty or not
                yield new int[]{0, 1};
            }
            case COMPLETE_COURSE_IN_EVERY_CATEGORY -> {
                long totalCategories = categoryRepository.count();
                long categoriesCompleted = lessonCompletionRepository.countDistinctCategoriesCompleted(userId);
                yield new int[]{(int) Math.min(categoriesCompleted, totalCategories), (int) Math.max(totalCategories, 1)};
            }
            case COMPLETE_ALL_IN_CATEGORY -> {
                if (a.getCategoryId() != null) {
                    long totalInCat = lessonRepository.countByCategoryId(a.getCategoryId());
                    long completedInCat = lessonCompletionRepository.countCompletedInCategory(userId, a.getCategoryId());
                    yield new int[]{(int) Math.min(completedInCat, Math.max(totalInCat, 1)), (int) Math.max(totalInCat, 1)};
                }
                yield new int[]{0, 1};
            }
            case COMPLETE_ALL_IN_EVERY_CATEGORY -> {
                long totalLessons = lessonRepository.count();
                long totalCompleted = lessonCompletionRepository.countByUserId(userId);
                yield new int[]{(int) Math.min(totalCompleted, Math.max(totalLessons, 1)), (int) Math.max(totalLessons, 1)};
            }
        };
    }

    // ── rank history (cursor-based) ──

    @Transactional(readOnly = true)
    public CursorPage<RankHistoryResponse> getRankHistory(UUID userId, String cursorStr, int limit) {
        limit = Math.min(Math.max(limit, 1), 50);
        RankHistoryCursor cursor = decodeCursor(cursorStr, RankHistoryCursor.class);

        OffsetDateTime cursorChangedAt = cursor != null ? cursor.t() : null;
        UUID cursorId = cursor != null ? cursor.i() : null;

        List<RankHistory> rows = rankHistoryRepository.findByUserIdAfterCursor(
                userId, cursorChangedAt, cursorId, PageRequest.of(0, limit + 1));

        List<RankHistoryResponse> edges = rows.stream()
                .limit(limit)
                .map(h -> {
                    RankHistoryResponse r = new RankHistoryResponse();
                    r.setId(h.getId());
                    r.setScope(h.getScope().name());
                    r.setOldRank(h.getOldRank().name());
                    r.setNewRank(h.getNewRank().name());
                    r.setChangedAt(h.getChangedAt());
                    return r;
                }).toList();

        boolean hasNext = rows.size() > limit;
        String endCursor = null;
        if (hasNext) {
            RankHistory last = rows.get(limit);
            endCursor = encodeCursor(new RankHistoryCursor(last.getChangedAt(), last.getId()));
        }

        return new CursorPage<>(edges, hasNext, endCursor);
    }

    // ── streak ──

    @Transactional(readOnly = true)
    public StreakResponse getStreak(UUID userId) {
        UserGamification user = userGamificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Gamification profile not found for user " + userId));

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

        Long cursorXp = cursor != null ? cursor.x() : null;
        UUID cursorUserId = cursor != null ? cursor.u() : null;
        int startRank = cursor != null ? cursor.r() : 0;

        List<UserGamification> rows = userGamificationRepository.findLeaderboardAfterCursor(
                cursorXp, cursorUserId, PageRequest.of(0, limit + 1));

        if (rows.isEmpty()) {
            return new CursorPage<>(List.of(), false, null);
        }

        List<UUID> userIds = rows.stream().map(UserGamification::getUserId).toList();
        Map<UUID, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // Build a sorted list for tier lookup (descending by minXp) — O(1) per row after sort
        List<RankTier> allTiers = new java.util.ArrayList<>(rankTierRepository.findAll());
        allTiers.sort((a, b) -> Long.compare(b.getMinXp(), a.getMinXp()));

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
                                                         List<RankTier> sortedTiers, int rank) {
        User user = usersById.get(ug.getUserId());

        // O(n) scan over sorted tiers, but n is tiny (6 tiers) and list is pre-sorted
        RankTier tier = sortedTiers.stream()
                .filter(t -> t.getMinXp() <= ug.getTotalXp())
                .findFirst()
                .orElse(null);

        LeaderboardEntryResponse e = new LeaderboardEntryResponse();
        e.setRank(rank + 1);
        e.setUserId(ug.getUserId());
        e.setDisplayName(user != null ? user.getDisplayName() : "Deleted User");
        e.setAvatarUrl(user != null ? user.getAvatarUrl() : null);
        e.setTotalXp(ug.getTotalXp());
        e.setTierName(tier != null ? tier.getTierName().name() : null);
        e.setTierIcon(tier != null ? tier.getIconUrl() : null);
        return e;
    }
}