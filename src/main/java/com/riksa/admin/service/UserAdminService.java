package com.riksa.admin.service;

import com.riksa.admin.dto.AdminUserResponse;
import com.riksa.admin.dto.AchievementGrantRequest;
import com.riksa.admin.dto.StreakAdjustmentRequest;
import com.riksa.admin.dto.SuspensionRequest;
import com.riksa.admin.dto.UserOverviewResponse;
import com.riksa.admin.dto.XpAdjustmentRequest;
import com.riksa.common.audit.AuditLogService;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.event.EventPublisher;
import com.riksa.common.cache.CacheNames;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.gamification.entity.Achievement;
import com.riksa.gamification.entity.UserAchievement;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.entity.XpTransaction;
import com.riksa.gamification.event.StreakUpdatedEvent;
import com.riksa.gamification.event.XpGainedEvent;
import com.riksa.gamification.repository.AchievementRepository;
import com.riksa.gamification.repository.RankTierRepository;
import com.riksa.gamification.repository.UserAchievementRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.repository.XpTransactionRepository;
import com.riksa.gamification.service.XpLedgerService;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.leaderboard.service.RedisLeaderboardProjector;
import com.riksa.report.entity.ContentReport;
import com.riksa.report.repository.ContentReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAdminService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final UserGamificationRepository userGamificationRepository;
    private final RankTierRepository rankTierRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final XpLedgerService xpLedgerService;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final ContentReportRepository contentReportRepository;
    private final EventPublisher eventPublisher;
    private final RedisLeaderboardProjector redisLeaderboardProjector;

    private static final int MAX_PAGE_SIZE = 100;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> list(String query, Role role, Boolean suspended, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        String safeQuery = (query == null || query.isBlank()) ? "" : query.trim();
        return userRepository.search(safeQuery, role, suspended, OffsetDateTime.now(), PageRequest.of(page, safeSize))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserOverviewResponse getOverview(UUID userId) {
        User user = requireUser(userId);
        List<UserAchievement> earned = userAchievementRepository.findByUserId(userId);
        Map<UUID, UserAchievement> earnedByAchievement = earned.stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, Function.identity(),
                        (first, ignored) -> first));

        List<UserOverviewResponse.AchievementOverview> achievements = achievementRepository.findAll().stream()
                .map(achievement -> toAchievementOverview(achievement,
                        earnedByAchievement.get(achievement.getId())))
                .toList();

        UserOverviewResponse.GamificationOverview gamification = userGamificationRepository
                .findByUserId(userId)
                .map(this::toGamificationOverview)
                .orElse(null);

        List<UserOverviewResponse.RecentCompletion> completions = lessonCompletionRepository
                .findRecentByUserId(userId, PageRequest.of(0, 10))
                .stream()
                .map(row -> new UserOverviewResponse.RecentCompletion(
                        row.getLessonId(), row.getLessonTitle(), row.getXpEarned(), row.getScore(), row.getCompletedAt()))
                .toList();

        List<UserOverviewResponse.ReportOverview> reportsAgainst = contentReportRepository
                .findReportsAgainstUserContent(userId)
                .stream()
                .map(this::toReportOverview)
                .toList();

        return new UserOverviewResponse(
                toResponse(user),
                user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts(),
                user.isLeaderboardExcluded(),
                gamification,
                achievements,
                lessonCompletionRepository.countByUserId(userId),
                completions,
                contentReportRepository.countByReporterId(userId),
                reportsAgainst
        );
    }

    @Transactional(readOnly = true)
    public Page<com.riksa.gamification.dto.XpTransactionResponse> xpHistory(UUID userId, int page, int size) {
        requireUser(userId);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        PageRequest pageable = PageRequest.of(Math.max(0, page), safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return xpTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toXpTransactionResponse);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.GAMIFICATION_PROFILE, key = "#userId.toString()")
    public UserOverviewResponse adjustXp(UUID actorId, UUID userId, XpAdjustmentRequest request) {
        requireUser(userId);
        int requestedAmount = request.getAmount();
        int appliedAmount = requestedAmount;

        if (requestedAmount > 0) {
            xpLedgerService.awardXp(userId, requestedAmount, "ADMIN_ADJUST", null);
        } else {
            // XpLedgerService currently rejects non-positive amounts. Keep removals in the
            // same locked ledger path and emit the normal XP event for downstream listeners.
            UserGamification gamification = userGamificationRepository.findByIdForUpdate(userId)
                    .orElseThrow(() -> new IllegalStateException(
                            "UserGamification row not found for user " + userId));
            long toRemove = Math.min(Math.abs((long) requestedAmount), Math.max(0L, gamification.getTotalXp()));
            appliedAmount = Math.toIntExact(-toRemove);
            if (toRemove > 0) {
                long newTotal = gamification.getTotalXp() - toRemove;
                gamification.setTotalXp(newTotal);
                gamification.setXpBalance(Math.max(0L, gamification.getXpBalance() - toRemove));
                userGamificationRepository.save(gamification);

                XpTransaction transaction = new XpTransaction();
                transaction.setUserId(userId);
                transaction.setAmount(appliedAmount);
                transaction.setSourceType("ADMIN_ADJUST");
                transaction.setSourceId(null);
                transaction.setRunningBalance(newTotal);
                xpTransactionRepository.save(transaction);

                eventPublisher.publish(new XpGainedEvent(this, userId, appliedAmount, newTotal,
                        "ADMIN_ADJUST", null));
            }
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", request.getReason());
        details.put("requestedAmount", requestedAmount);
        details.put("appliedAmount", appliedAmount);
        auditLogService.record(actorId, "USER_XP_ADJUST", "USER", userId, details);
        return getOverview(userId);
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheNames.GAMIFICATION_PROFILE, CacheNames.GAMIFICATION_STREAK,
            CacheNames.GAMIFICATION_ACHIEVEMENTS}, key = "#userId.toString()")
    public UserOverviewResponse adjustStreak(UUID actorId, UUID userId, StreakAdjustmentRequest request) {
        requireUser(userId);
        UserGamification gamification = userGamificationRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "UserGamification row not found for user " + userId));

        long oldCurrent = gamification.getCurrentStreak();
        long oldHighest = gamification.getHighestStreak();
        long current = request.getCurrentStreak();
        long highest = Math.max(oldHighest, current);
        gamification.setCurrentStreak(current);
        gamification.setHighestStreak(highest);
        userGamificationRepository.save(gamification);
        eventPublisher.publish(new StreakUpdatedEvent(this, userId, current, highest));

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", request.getReason());
        details.put("previousCurrentStreak", oldCurrent);
        details.put("currentStreak", current);
        details.put("previousHighestStreak", oldHighest);
        details.put("highestStreak", highest);
        auditLogService.record(actorId, "USER_STREAK_ADJUST", "USER", userId, details);
        return getOverview(userId);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.GAMIFICATION_ACHIEVEMENTS, key = "#userId.toString()")
    public UserOverviewResponse grantAchievement(UUID actorId, UUID userId, UUID achievementId,
                                                 AchievementGrantRequest request) {
        requireUser(userId);
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement not found: " + achievementId));
        boolean alreadyEarned = userAchievementRepository
                .existsByUserIdAndAchievementId(userId, achievementId);
        if (!alreadyEarned) {
            UserAchievement earned = new UserAchievement();
            earned.setUserId(userId);
            earned.setAchievementId(achievementId);
            userAchievementRepository.save(earned);
        }

        auditLogService.record(actorId, "USER_ACHIEVEMENT_GRANT", "USER", userId,
                Map.of("achievementId", achievementId.toString(), "achievementName", achievement.getName(),
                        "reason", request.getReason(), "alreadyEarned", alreadyEarned,
                        "xpRewardGranted", false));
        return getOverview(userId);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.GAMIFICATION_ACHIEVEMENTS, key = "#userId.toString()")
    public UserOverviewResponse revokeAchievement(UUID actorId, UUID userId, UUID achievementId, String reason) {
        requireUser(userId);
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement not found: " + achievementId));
        var earned = userAchievementRepository.findByUserIdAndAchievementId(userId, achievementId);
        earned.ifPresent(userAchievementRepository::delete);

        auditLogService.record(actorId, "USER_ACHIEVEMENT_REVOKE", "USER", userId,
                Map.of("achievementId", achievementId.toString(), "achievementName", achievement.getName(),
                        "reason", reason, "wasEarned", earned.isPresent(), "xpAdjusted", false));
        return getOverview(userId);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.USER_SECURITY_STATE, key = "#userId.toString()")
    public UserOverviewResponse unlock(UUID actorId, UUID userId) {
        User user = requireUser(userId);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("previousFailedLoginAttempts", user.getFailedLoginAttempts());
        details.put("previousLockedUntil", user.getLockedUntil() == null ? null : user.getLockedUntil().toString());
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        auditLogService.record(actorId, "USER_UNLOCK", "USER", userId, details);
        return getOverview(userId);
    }

    @Transactional
    public UserOverviewResponse setLeaderboardExclusion(UUID actorId, UUID userId, boolean excluded, String reason) {
        User user = requireUser(userId);
        boolean previous = user.isLeaderboardExcluded();
        user.setLeaderboardExcluded(excluded);
        userRepository.save(user);

        try {
            redisLeaderboardProjector.update(userId);
        } catch (RuntimeException error) {
            log.warn("Could not immediately update Redis leaderboard visibility for user {}", userId, error);
        }

        auditLogService.record(actorId, "USER_LEADERBOARD_EXCLUSION", "USER", userId,
                Map.of("previousExcluded", previous, "excluded", excluded, "reason", reason));
        return getOverview(userId);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.USER_SECURITY_STATE, key = "#userId.toString()")
    public AdminUserResponse updateRole(UUID actorId, UUID userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (newRole == Role.USER) {
            throw new IllegalArgumentException("Cannot set role to USER via admin endpoint");
        }

        Role oldRole = user.getRole();
        user.setRole(newRole);
        user = userRepository.save(user);

        auditLogService.record(actorId, "USER_ROLE_CHANGE", "USER", userId,
                Map.of("oldRole", oldRole.name(), "newRole", newRole.name()));

        return toResponse(user);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.USER_SECURITY_STATE, key = "#userId.toString()")
    public AdminUserResponse suspend(UUID actorId, UUID userId, SuspensionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getRole() == Role.MODERATOR || user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot suspend staff accounts");
        }
        if (request.getUntil().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Suspension end date must be in the future");
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", request.getReason());
        details.put("until", request.getUntil().toString());
        if (user.getSuspensionReason() != null) {
            details.put("previousReason", user.getSuspensionReason());
        }

        user.setSuspendedUntil(request.getUntil());
        user.setSuspensionReason(request.getReason());
        user = userRepository.save(user);

        auditLogService.record(actorId, "USER_SUSPEND", "USER", userId, details);

        return toResponse(user);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.USER_SECURITY_STATE, key = "#userId.toString()")
    public AdminUserResponse unsuspend(UUID actorId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!user.isSuspended()) {
            throw new IllegalArgumentException("User is not suspended");
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("previousReason", user.getSuspensionReason());
        details.put("previousUntil", user.getSuspendedUntil().toString());

        user.setSuspendedUntil(null);
        user.setSuspensionReason(null);
        user = userRepository.save(user);

        auditLogService.record(actorId, "USER_UNSUSPEND", "USER", userId, details);

        return toResponse(user);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getEmailVerified(),
                user.getAvatarUrl(),
                user.getRole().name(),
                user.getSuspendedUntil(),
                user.getSuspensionReason(),
                user.getLockedUntil(),
                user.getCreatedAt()
        );
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private UserOverviewResponse.GamificationOverview toGamificationOverview(UserGamification gamification) {
        var tier = rankTierRepository.findFirstByMinXpLessThanEqualOrderByMinXpDesc(gamification.getTotalXp())
                .orElse(null);
        return new UserOverviewResponse.GamificationOverview(
                gamification.getTotalXp(),
                gamification.getCurrentStreak(),
                gamification.getHighestStreak(),
                gamification.getLastCompletedAt(),
                tier == null ? null : tier.getTierName().name(),
                tier == null ? null : tier.getIconUrl()
        );
    }

    private UserOverviewResponse.AchievementOverview toAchievementOverview(
            Achievement achievement, UserAchievement earned) {
        return new UserOverviewResponse.AchievementOverview(
                achievement.getId(), achievement.getName(), achievement.getIconUrl(),
                achievement.getRarity().name(), achievement.getXpReward(), achievement.isActive(),
                earned != null, earned == null ? null : earned.getUnlockedAt());
    }

    private UserOverviewResponse.ReportOverview toReportOverview(ContentReport report) {
        return new UserOverviewResponse.ReportOverview(
                report.getId(), report.getTargetType(), report.getTargetId(), report.getReason(),
                report.getDetails(), report.getStatus(), report.getCreatedAt());
    }

    private com.riksa.gamification.dto.XpTransactionResponse toXpTransactionResponse(XpTransaction transaction) {
        var response = new com.riksa.gamification.dto.XpTransactionResponse();
        response.setId(transaction.getId());
        response.setAmount(transaction.getAmount());
        response.setSourceType(transaction.getSourceType());
        response.setSourceId(transaction.getSourceId());
        response.setRunningBalance(transaction.getRunningBalance());
        response.setCreatedAt(transaction.getCreatedAt());
        return response;
    }
}
