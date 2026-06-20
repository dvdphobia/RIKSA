package com.riksa.gamification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.enums.AchievementCriteriaType;
import com.riksa.common.enums.ScopeType;
import com.riksa.common.enums.TierType;
import com.riksa.common.exception.InvalidCursorException;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.gamification.dto.*;
import com.riksa.gamification.entity.*;
import com.riksa.gamification.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock private UserGamificationRepository userGamificationRepository;
    @Mock private RankTierRepository rankTierRepository;
    @Mock private XpTransactionRepository xpTransactionRepository;
    @Mock private AchievementRepository achievementRepository;
    @Mock private UserAchievementRepository userAchievementRepository;
    @Mock private RankHistoryRepository rankHistoryRepository;
    @Mock private StreakMilestoneRepository streakMilestoneRepository;
    @Mock private UserRepository userRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private GamificationService gamificationService;

    // ── getProfile ──

    @Test
    void getProfile_userNotFound_throwsResourceNotFound() {
        UUID userId = UUID.randomUUID();
        when(userGamificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gamificationService.getProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Gamification profile not found");
    }

    @Test
    void getProfile_returnsCorrectProfile() {
        UUID userId = UUID.randomUUID();
        UserGamification ug = new UserGamification();
        ug.setUserId(userId);
        ug.setTotalXp(550);
        ug.setCurrentStreak(7);
        ug.setHighestStreak(14);

        RankTier gold = new RankTier();
        gold.setTierName(TierType.GOLD);
        gold.setMinXp(500);
        gold.setIconUrl("/icons/rank-gold.png");

        RankTier platinum = new RankTier();
        platinum.setTierName(TierType.PLATINUM);
        platinum.setMinXp(1000);
        platinum.setIconUrl("/icons/rank-platinum.png");

        when(userGamificationRepository.findByUserId(userId)).thenReturn(Optional.of(ug));
        when(rankTierRepository.findFirstByMinXpLessThanEqualOrderByMinXpDesc(550L)).thenReturn(Optional.of(gold));
        when(rankTierRepository.findAll()).thenReturn(List.of(gold, platinum));

        GamificationProfileResponse response = gamificationService.getProfile(userId);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getTotalXp()).isEqualTo(550);
        assertThat(response.getCurrentTier()).isEqualTo("GOLD");
        assertThat(response.getNextTier()).isEqualTo("PLATINUM");
        assertThat(response.getXpToNext()).isEqualTo(450); // 1000 - 550
    }

    @Test
    void getProfile_maxRank_noNextTier() {
        UUID userId = UUID.randomUUID();
        UserGamification ug = new UserGamification();
        ug.setUserId(userId);
        ug.setTotalXp(10000);
        ug.setCurrentStreak(30);
        ug.setHighestStreak(30);

        RankTier legend = new RankTier();
        legend.setTierName(TierType.LEGEND);
        legend.setMinXp(5000);
        legend.setIconUrl("/icons/rank-legend.png");

        when(userGamificationRepository.findByUserId(userId)).thenReturn(Optional.of(ug));
        when(rankTierRepository.findFirstByMinXpLessThanEqualOrderByMinXpDesc(10000L)).thenReturn(Optional.of(legend));
        when(rankTierRepository.findAll()).thenReturn(List.of(legend));

        GamificationProfileResponse response = gamificationService.getProfile(userId);

        assertThat(response.getCurrentTier()).isEqualTo("LEGEND");
        assertThat(response.getNextTier()).isNull();
        assertThat(response.getXpToNext()).isEqualTo(0);
    }

    // ── getXpHistory ──

    @Test
    void getXpHistory_noCursor_returnsFirstPage() {
        UUID userId = UUID.randomUUID();
        XpTransaction tx = new XpTransaction();
        tx.setId(UUID.randomUUID());
        tx.setUserId(userId);
        tx.setAmount(50);
        tx.setSourceType("LESSON_COMPLETION");
        tx.setSourceId(UUID.randomUUID());
        tx.setRunningBalance(50);
        tx.setCreatedAt(OffsetDateTime.now());

        when(xpTransactionRepository.findByUserIdAfterCursor(eq(userId), isNull(), isNull(), any()))
                .thenReturn(List.of(tx));

        CursorPage<XpTransactionResponse> page = gamificationService.getXpHistory(userId, null, 20);

        assertThat(page.getEdges()).hasSize(1);
        assertThat(page.isHasNextPage()).isFalse();
        assertThat(page.getEndCursor()).isNull();
    }

    @Test
    void getXpHistory_invalidCursor_throwsInvalidCursorException() {
        UUID userId = UUID.randomUUID();
        assertThatThrownBy(() -> gamificationService.getXpHistory(userId, "not-valid-base64!!", 20))
                .isInstanceOf(InvalidCursorException.class);
    }

    // ── getStreak ──

    @Test
    void getStreak_userNotFound_throwsResourceNotFound() {
        UUID userId = UUID.randomUUID();
        when(userGamificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gamificationService.getStreak(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getStreak_returnsCorrectMilestones() {
        UUID userId = UUID.randomUUID();
        UserGamification ug = new UserGamification();
        ug.setUserId(userId);
        ug.setCurrentStreak(7);
        ug.setHighestStreak(14);

        StreakMilestone weekWarrior = new StreakMilestone();
        weekWarrior.setMinStreak(7);
        weekWarrior.setStreakName("Week Warrior");
        weekWarrior.setIconUrl("/icons/streak-7.png");

        StreakMilestone twoWeek = new StreakMilestone();
        twoWeek.setMinStreak(14);
        twoWeek.setStreakName("Two Week Streak");
        twoWeek.setIconUrl("/icons/streak-14.png");

        when(userGamificationRepository.findByUserId(userId)).thenReturn(Optional.of(ug));
        when(streakMilestoneRepository.findFirstByMinStreakLessThanEqualOrderByMinStreakDesc(7))
                .thenReturn(Optional.of(weekWarrior));
        when(streakMilestoneRepository.findAll()).thenReturn(List.of(weekWarrior, twoWeek));

        StreakResponse response = gamificationService.getStreak(userId);

        assertThat(response.getCurrentStreak()).isEqualTo(7);
        assertThat(response.getCurrentMilestoneName()).isEqualTo("Week Warrior");
        assertThat(response.getNextMilestoneName()).isEqualTo("Two Week Streak");
        assertThat(response.getDaysToNext()).isEqualTo(7); // 14 - 7
    }

    // ── getAchievements ──

    @Test
    void getAchievements_marksEarnedCorrectly() {
        UUID userId = UUID.randomUUID();
        UUID achievementId = UUID.randomUUID();

        Achievement ach = new Achievement();
        ach.setId(achievementId);
        ach.setName("First Lesson");
        ach.setDescription("Complete your first lesson");
        ach.setCriteriaType(AchievementCriteriaType.COMPLETE_N_COURSES);
        ach.setCriteriaValue(1L);
        ach.setIconUrl("/icons/ach-first-lesson.png");

        UserAchievement ua = new UserAchievement();
        ua.setUserId(userId);
        ua.setAchievementId(achievementId);
        ua.setUnlockedAt(OffsetDateTime.now());

        when(achievementRepository.findAll()).thenReturn(List.of(ach));
        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of(ua));

        List<AchievementResponse> result = gamificationService.getAchievements(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isEarned()).isTrue();
        assertThat(result.get(0).getUnlockedAt()).isNotNull();
    }

    // ── getRankHistory ──

    @Test
    void getRankHistory_noHistory_returnsEmptyPage() {
        UUID userId = UUID.randomUUID();
        when(rankHistoryRepository.findByUserIdAfterCursor(eq(userId), isNull(), isNull(), any()))
                .thenReturn(List.of());

        CursorPage<RankHistoryResponse> page = gamificationService.getRankHistory(userId, null, 20);

        assertThat(page.getEdges()).isEmpty();
        assertThat(page.isHasNextPage()).isFalse();
    }

    @Test
    void getRankHistory_withEntries_returnsCorrectly() {
        UUID userId = UUID.randomUUID();
        RankHistory rh = new RankHistory();
        rh.setId(UUID.randomUUID());
        rh.setUserId(userId);
        rh.setScope(ScopeType.COUNTRY);
        rh.setOldRank(TierType.BRONZE);
        rh.setNewRank(TierType.SILVER);
        rh.setChangedAt(OffsetDateTime.now());

        when(rankHistoryRepository.findByUserIdAfterCursor(eq(userId), isNull(), isNull(), any()))
                .thenReturn(List.of(rh));

        CursorPage<RankHistoryResponse> page = gamificationService.getRankHistory(userId, null, 20);

        assertThat(page.getEdges()).hasSize(1);
        assertThat(page.getEdges().get(0).getOldRank()).isEqualTo("BRONZE");
        assertThat(page.getEdges().get(0).getNewRank()).isEqualTo("SILVER");
    }

    // ── getLeaderboard ──

    @Test
    void getLeaderboard_empty_returnsEmptyPage() {
        when(userGamificationRepository.findLeaderboardAfterCursor(isNull(), isNull(), any()))
                .thenReturn(List.of());

        CursorPage<LeaderboardEntryResponse> page = gamificationService.getLeaderboard(null, 20);

        assertThat(page.getEdges()).isEmpty();
        assertThat(page.isHasNextPage()).isFalse();
    }

    @Test
    void getLeaderboard_deletedUser_showsDeletedUser() {
        UUID userId = UUID.randomUUID();
        UserGamification ug = new UserGamification();
        ug.setUserId(userId);
        ug.setTotalXp(100);

        RankTier bronze = new RankTier();
        bronze.setTierName(TierType.BRONZE);
        bronze.setMinXp(0);
        bronze.setIconUrl("/icons/rank-bronze.png");

        when(userGamificationRepository.findLeaderboardAfterCursor(isNull(), isNull(), any()))
                .thenReturn(List.of(ug));
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of()); // User deleted
        when(rankTierRepository.findAll()).thenReturn(List.of(bronze));

        CursorPage<LeaderboardEntryResponse> page = gamificationService.getLeaderboard(null, 20);

        assertThat(page.getEdges()).hasSize(1);
        assertThat(page.getEdges().get(0).getDisplayName()).isEqualTo("Deleted User");
    }
}