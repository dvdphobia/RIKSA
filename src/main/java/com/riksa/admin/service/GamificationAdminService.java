package com.riksa.admin.service;

import com.riksa.admin.dto.AchievementAdminRequest;
import com.riksa.admin.dto.AchievementAdminResponse;
import com.riksa.admin.dto.RankTierAdminResponse;
import com.riksa.admin.dto.RankTierUpdateRequest;
import com.riksa.admin.dto.StreakMilestoneAdminResponse;
import com.riksa.admin.dto.StreakMilestoneRequest;
import com.riksa.admin.dto.XpAuditRow;
import com.riksa.admin.dto.XpBoostAdminResponse;
import com.riksa.admin.dto.XpBoostRequest;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.audit.AuditLogService;
import com.riksa.common.cache.CacheNames;
import com.riksa.common.enums.TierType;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.gamification.entity.Achievement;
import com.riksa.gamification.entity.RankTier;
import com.riksa.gamification.entity.StreakMilestone;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.entity.XpBoost;
import com.riksa.gamification.repository.AchievementRepository;
import com.riksa.gamification.repository.RankTierRepository;
import com.riksa.gamification.repository.StreakMilestoneRepository;
import com.riksa.gamification.repository.UserAchievementRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.repository.XpBoostRepository;
import com.riksa.gamification.repository.XpTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GamificationAdminService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final RankTierRepository rankTierRepository;
    private final UserGamificationRepository userGamificationRepository;
    private final StreakMilestoneRepository streakMilestoneRepository;
    private final XpBoostRepository xpBoostRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<AchievementAdminResponse> listAchievements() {
        return achievementRepository.findAll().stream()
                .map(this::achievementResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.GAMIFICATION_ACHIEVEMENTS, allEntries = true,
            condition = "@redisCacheFeatureSwitch.enabled()")
    public List<AchievementAdminResponse> createAchievement(UUID actorId, AchievementAdminRequest request) {
        if (achievementRepository.existsByName(request.name().trim())) {
            throw new IllegalArgumentException("Achievement name already exists: " + request.name());
        }
        validateAchievement(request);
        Achievement achievement = new Achievement();
        apply(achievement, request);
        achievement = achievementRepository.save(achievement);
        auditLogService.record(actorId, "ACHIEVEMENT_CREATE", "ACHIEVEMENT", achievement.getId(),
                Map.of("name", achievement.getName(), "criteriaType", achievement.getCriteriaType().name()));
        return listAchievements();
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.GAMIFICATION_ACHIEVEMENTS, allEntries = true,
            condition = "@redisCacheFeatureSwitch.enabled()")
    public List<AchievementAdminResponse> updateAchievement(UUID actorId, UUID id,
                                                              AchievementAdminRequest request) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement not found: " + id));
        if (achievementRepository.existsByNameAndIdNot(request.name().trim(), id)) {
            throw new IllegalArgumentException("Achievement name already exists: " + request.name());
        }
        validateAchievement(request);
        apply(achievement, request);
        achievementRepository.save(achievement);
        auditLogService.record(actorId, "ACHIEVEMENT_UPDATE", "ACHIEVEMENT", id,
                Map.of("name", achievement.getName(), "active", achievement.isActive()));
        return listAchievements();
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.GAMIFICATION_ACHIEVEMENTS, allEntries = true,
            condition = "@redisCacheFeatureSwitch.enabled()")
    public List<AchievementAdminResponse> deleteAchievement(UUID actorId, UUID id) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement not found: " + id));
        long earnedCount = userAchievementRepository.countByAchievementId(id);
        if (earnedCount > 0) {
            throw new IllegalStateException("Achievement has already been earned; deactivate it instead");
        }
        achievementRepository.delete(achievement);
        auditLogService.record(actorId, "ACHIEVEMENT_DELETE", "ACHIEVEMENT", id,
                Map.of("name", achievement.getName()));
        return listAchievements();
    }

    @Transactional(readOnly = true)
    public List<RankTierAdminResponse> listRankTiers() {
        List<RankTier> tiers = rankTierRepository.findAllByOrderByMinXpAsc();
        return java.util.stream.IntStream.range(0, tiers.size())
                .mapToObj(index -> {
                    RankTier tier = tiers.get(index);
                    long nextThreshold = index + 1 < tiers.size()
                            ? tiers.get(index + 1).getMinXp() : Long.MAX_VALUE;
                    return rankTierResponse(tier, nextThreshold);
                })
                .toList();
    }

    @Transactional
    public List<RankTierAdminResponse> updateRankTier(UUID actorId, UUID id, RankTierUpdateRequest request) {
        RankTier tier = rankTierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rank tier not found: " + id));
        tier.setMinXp(request.minXp());
        tier.setIconUrl(request.iconUrl().trim());

        List<RankTier> allTiers = rankTierRepository.findAll();
        validateTierThresholds(allTiers);
        rankTierRepository.save(tier);
        auditLogService.record(actorId, "RANK_TIER_UPDATE", "RANK_TIER", id,
                Map.of("tierName", tier.getTierName().name(), "minXp", tier.getMinXp()));
        return listRankTiers();
    }

    @Transactional(readOnly = true)
    public List<StreakMilestoneAdminResponse> listStreakMilestones() {
        return streakMilestoneRepository.findAllByOrderByMinStreakAsc().stream()
                .map(this::streakResponse)
                .toList();
    }

    @Transactional
    public List<StreakMilestoneAdminResponse> createStreakMilestone(UUID actorId,
                                                                     StreakMilestoneRequest request) {
        if (streakMilestoneRepository.existsByMinStreak(request.minStreak())) {
            throw new IllegalArgumentException("A streak milestone already exists at this day");
        }
        StreakMilestone milestone = new StreakMilestone();
        apply(milestone, request);
        milestone = streakMilestoneRepository.save(milestone);
        auditLogService.record(actorId, "STREAK_MILESTONE_CREATE", "STREAK_MILESTONE", milestone.getId(),
                Map.of("minStreak", milestone.getMinStreak(), "streakName", milestone.getStreakName()));
        return listStreakMilestones();
    }

    @Transactional
    public List<StreakMilestoneAdminResponse> updateStreakMilestone(UUID actorId, UUID id,
                                                                     StreakMilestoneRequest request) {
        StreakMilestone milestone = streakMilestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Streak milestone not found: " + id));
        if (streakMilestoneRepository.existsByMinStreakAndIdNot(request.minStreak(), id)) {
            throw new IllegalArgumentException("A streak milestone already exists at this day");
        }
        apply(milestone, request);
        streakMilestoneRepository.save(milestone);
        auditLogService.record(actorId, "STREAK_MILESTONE_UPDATE", "STREAK_MILESTONE", id,
                Map.of("minStreak", milestone.getMinStreak(), "streakName", milestone.getStreakName()));
        return listStreakMilestones();
    }

    @Transactional
    public List<StreakMilestoneAdminResponse> deleteStreakMilestone(UUID actorId, UUID id) {
        StreakMilestone milestone = streakMilestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Streak milestone not found: " + id));
        streakMilestoneRepository.delete(milestone);
        auditLogService.record(actorId, "STREAK_MILESTONE_DELETE", "STREAK_MILESTONE", id,
                Map.of("minStreak", milestone.getMinStreak(), "streakName", milestone.getStreakName()));
        return listStreakMilestones();
    }

    @Transactional(readOnly = true)
    public List<XpBoostAdminResponse> listXpBoosts() {
        OffsetDateTime now = OffsetDateTime.now();
        return xpBoostRepository.findAllByOrderByStartsAtAsc().stream()
                .map(boost -> boostResponse(boost, now))
                .toList();
    }

    @Transactional
    public List<XpBoostAdminResponse> createXpBoost(UUID actorId, XpBoostRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new IllegalArgumentException("Boost end must be after its start");
        }
        if (xpBoostRepository.existsByStartsAtBeforeAndEndsAtAfter(request.endsAt(), request.startsAt())) {
            throw new IllegalArgumentException("XP boost overlaps another scheduled or active boost");
        }
        XpBoost boost = new XpBoost();
        boost.setName(request.name().trim());
        boost.setMultiplier(request.multiplier());
        boost.setStartsAt(request.startsAt());
        boost.setEndsAt(request.endsAt());
        boost.setCreatedBy(actorId);
        boost = xpBoostRepository.save(boost);
        auditLogService.record(actorId, "XP_BOOST_CREATE", "XP_BOOST", boost.getId(),
                Map.of("name", boost.getName(), "multiplier", boost.getMultiplier(),
                        "startsAt", boost.getStartsAt().toString(), "endsAt", boost.getEndsAt().toString()));
        return listXpBoosts();
    }

    @Transactional
    public List<XpBoostAdminResponse> cancelXpBoost(UUID actorId, UUID id) {
        XpBoost boost = xpBoostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("XP boost not found: " + id));
        OffsetDateTime now = OffsetDateTime.now();
        String status = status(boost, now);
        if ("ENDED".equals(status)) {
            throw new IllegalStateException("XP boost has already ended");
        }
        OffsetDateTime previousStart = boost.getStartsAt();
        OffsetDateTime previousEnd = boost.getEndsAt();
        if ("SCHEDULED".equals(status)) {
            boost.setStartsAt(now.minusSeconds(1));
        }
        boost.setEndsAt(now);
        xpBoostRepository.save(boost);
        auditLogService.record(actorId, "XP_BOOST_CANCEL", "XP_BOOST", id,
                Map.of("name", boost.getName(), "previousStartsAt", previousStart.toString(),
                        "previousEndsAt", previousEnd.toString()));
        return listXpBoosts();
    }

    @Transactional(readOnly = true)
    public List<XpAuditRow> xpAudit(int hours, long minXp, int limit) {
        if (hours < 1 || hours > 24 * 30) {
            throw new IllegalArgumentException("hours must be between 1 and 720");
        }
        if (minXp < 0) {
            throw new IllegalArgumentException("minXp must be zero or greater");
        }
        int safeLimit = Math.max(1, Math.min(limit, 200));
        OffsetDateTime until = OffsetDateTime.now();
        List<XpTransactionRepository.XpAuditAggregate> aggregates = xpTransactionRepository
                .findXpAuditAggregates(until.minusHours(hours), until, minXp, PageRequest.of(0, safeLimit));
        if (aggregates.isEmpty()) return List.of();

        List<UUID> userIds = aggregates.stream().map(XpTransactionRepository.XpAuditAggregate::getUserId).toList();
        Map<UUID, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        Map<UUID, UserGamification> gamification = userGamificationRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserGamification::getUserId, user -> user));

        return aggregates.stream().filter(row -> users.containsKey(row.getUserId())).map(row -> {
            User user = users.get(row.getUserId());
            UserGamification state = gamification.get(row.getUserId());
            return new XpAuditRow(row.getUserId(), user.getUsername(), user.getDisplayName(),
                    row.getXpGained(), row.getTransactionCount(), row.getLessonCompletions(),
                    state == null ? 0 : state.getTotalXp(), row.getFirstAt(), row.getLastAt(),
                    user.isSuspended(), user.isLeaderboardExcluded());
        }).toList();
    }

    private void apply(Achievement achievement, AchievementAdminRequest request) {
        achievement.setName(request.name().trim());
        achievement.setDescription(request.description().trim());
        achievement.setCriteriaType(request.criteriaType());
        achievement.setCriteriaValue(request.criteriaValue());
        achievement.setRequiresAdvanced(request.requiresAdvanced());
        achievement.setIconUrl(request.iconUrl().trim());
        achievement.setCategoryId(request.categoryId());
        achievement.setRarity(request.rarity());
        achievement.setXpReward(request.xpReward());
        achievement.setActive(request.active());
    }

    private void validateAchievement(AchievementAdminRequest request) {
        if (request.criteriaType() == com.riksa.common.enums.AchievementCriteriaType.COMPLETE_ALL_IN_CATEGORY
                && request.categoryId() == null) {
            throw new IllegalArgumentException("A category is required for COMPLETE_ALL_IN_CATEGORY achievements");
        }
    }

    private void validateTierThresholds(List<RankTier> tiers) {
        Map<TierType, Long> thresholds = new EnumMap<>(TierType.class);
        tiers.forEach(tier -> thresholds.put(tier.getTierName(), tier.getMinXp()));
        if (!thresholds.containsKey(TierType.BRONZE) || thresholds.get(TierType.BRONZE) != 0L) {
            throw new IllegalArgumentException("The first rank tier must start at 0 XP");
        }
        Long previous = null;
        for (TierType type : TierType.values()) {
            Long threshold = thresholds.get(type);
            if (threshold == null) continue;
            if (previous != null && threshold <= previous) {
                throw new IllegalArgumentException("Rank tier XP thresholds must rise tier by tier without duplicates");
            }
            previous = threshold;
        }
    }

    private void apply(StreakMilestone milestone, StreakMilestoneRequest request) {
        milestone.setMinStreak(request.minStreak());
        milestone.setStreakName(request.streakName().trim());
        milestone.setIconUrl(request.iconUrl().trim());
    }

    private AchievementAdminResponse achievementResponse(Achievement achievement) {
        return new AchievementAdminResponse(achievement.getId(), achievement.getName(), achievement.getDescription(),
                achievement.getCriteriaType(), achievement.getCriteriaValue(), achievement.isRequiresAdvanced(),
                achievement.getIconUrl(), achievement.getCategoryId(), achievement.getRarity(),
                achievement.getXpReward(), achievement.isActive(),
                userAchievementRepository.countByAchievementId(achievement.getId()), achievement.getCreatedAt());
    }

    private RankTierAdminResponse rankTierResponse(RankTier tier, long nextThreshold) {
        long users = nextThreshold == Long.MAX_VALUE
                ? userGamificationRepository.countByTotalXpGreaterThanEqual(tier.getMinXp())
                : userGamificationRepository.countByTotalXpGreaterThanEqualAndTotalXpLessThan(
                        tier.getMinXp(), nextThreshold);
        return new RankTierAdminResponse(tier.getId(), tier.getTierName().name(), tier.getMinXp(),
                tier.getIconUrl(), users);
    }

    private StreakMilestoneAdminResponse streakResponse(StreakMilestone milestone) {
        return new StreakMilestoneAdminResponse(milestone.getId(), milestone.getMinStreak(),
                milestone.getStreakName(), milestone.getIconUrl());
    }

    private XpBoostAdminResponse boostResponse(XpBoost boost, OffsetDateTime now) {
        return new XpBoostAdminResponse(boost.getId(), boost.getName(), boost.getMultiplier(),
                boost.getStartsAt(), boost.getEndsAt(), status(boost, now), boost.getCreatedBy());
    }

    private String status(XpBoost boost, OffsetDateTime now) {
        if (now.isBefore(boost.getStartsAt())) return "SCHEDULED";
        if (now.isBefore(boost.getEndsAt())) return "ACTIVE";
        return "ENDED";
    }
}
