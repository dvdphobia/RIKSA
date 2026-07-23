package com.riksa.gamification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.blog.repository.BlogLikeRepository;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.common.enums.AchievementCriteriaType;
import com.riksa.common.enums.TierType;
import com.riksa.gamification.dto.*;
import com.riksa.gamification.entity.*;
import com.riksa.gamification.repository.*;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.social.repository.CategoryRepository;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.ReactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    // ── repos for achievement progress computation ──
    @Mock private LessonCompletionRepository lessonCompletionRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PostRepository postRepository;
    @Mock private ReactionRepository reactionRepository;
    @Mock private BlogPostRepository blogPostRepository;
    @Mock private BlogLikeRepository blogLikeRepository;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    @InjectMocks private GamificationService gamificationService;

    @Test
    void getProfile_existingUser_returnsProfile() {
        UUID userId = UUID.randomUUID();
        UserGamification ug = new UserGamification();
        ug.setUserId(userId);
        ug.setTotalXp(500);
        ug.setCurrentStreak(3);
        ug.setHighestStreak(10);

        RankTier bronze = new RankTier();
        bronze.setTierName(TierType.BRONZE);
        bronze.setMinXp(0);
        bronze.setIconUrl("bronze.png");

        RankTier silver = new RankTier();
        silver.setTierName(TierType.SILVER);
        silver.setMinXp(1000);
        silver.setIconUrl("silver.png");

        when(userGamificationRepository.findByUserId(userId)).thenReturn(Optional.of(ug));
        when(rankTierRepository.findFirstByMinXpLessThanEqualOrderByMinXpDesc(500))
                .thenReturn(Optional.of(bronze));
        when(rankTierRepository.findAll()).thenReturn(List.of(bronze, silver));

        GamificationProfileResponse response = gamificationService.getProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.getTotalXp()).isEqualTo(500);
        assertThat(response.getCurrentTier()).isEqualTo("BRONZE");
        assertThat(response.getNextTier()).isEqualTo("SILVER");
        assertThat(response.getXpToNext()).isEqualTo(500);
    }

    @Test
    void getProfile_nonExistentUser_throwsException() {
        UUID userId = UUID.randomUUID();
        when(userGamificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gamificationService.getProfile(userId))
                .isInstanceOf(com.riksa.common.exception.ResourceNotFoundException.class);
    }

    @Test
    void getProfile_maxTier_noNextTier() {
        UUID userId = UUID.randomUUID();
        UserGamification ug = new UserGamification();
        ug.setUserId(userId);
        ug.setTotalXp(10000);

        RankTier diamond = new RankTier();
        diamond.setTierName(TierType.DIAMOND);
        diamond.setMinXp(5000);
        diamond.setIconUrl("diamond.png");

        when(userGamificationRepository.findByUserId(userId)).thenReturn(Optional.of(ug));
        when(rankTierRepository.findFirstByMinXpLessThanEqualOrderByMinXpDesc(10000))
                .thenReturn(Optional.of(diamond));
        when(rankTierRepository.findAll()).thenReturn(List.of(diamond));

        GamificationProfileResponse response = gamificationService.getProfile(userId);

        assertThat(response.getNextTier()).isNull();
        assertThat(response.getXpToNext()).isEqualTo(0);
    }

    @Test
    void getXpHistory_noCursor_returnsFirstPage() {
        UUID userId = UUID.randomUUID();
        XpTransaction tx1 = new XpTransaction();
        tx1.setId(UUID.randomUUID());
        tx1.setUserId(userId);
        tx1.setAmount(50);
        tx1.setSourceType("LESSON");
        tx1.setRunningBalance(50);
        tx1.setCreatedAt(OffsetDateTime.now());

        when(xpTransactionRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(tx1)));

        CursorPage<XpTransactionResponse> result = gamificationService.getXpHistory(userId, null, 10);

        assertThat(result.getEdges()).hasSize(1);
        assertThat(result.isHasNextPage()).isFalse();
        assertThat(result.getEndCursor()).isNull();
    }

    @Test
    void getXpHistory_hasMorePages_returnsCursor() throws Exception {
        UUID userId = UUID.randomUUID();
        XpTransaction tx1 = new XpTransaction();
        tx1.setId(UUID.randomUUID());
        tx1.setUserId(userId);
        tx1.setAmount(50);
        tx1.setSourceType("LESSON");
        tx1.setRunningBalance(50);
        tx1.setCreatedAt(OffsetDateTime.now());

        XpTransaction tx2 = new XpTransaction();
        tx2.setId(UUID.randomUUID());
        tx2.setUserId(userId);
        tx2.setAmount(30);
        tx2.setSourceType("TASK");
        tx2.setRunningBalance(80);
        tx2.setCreatedAt(OffsetDateTime.now().minusHours(1));

        when(xpTransactionRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(tx1, tx2)));

        // Stub the spy's writeValueAsBytes to handle OffsetDateTime
        doReturn("{\"t\":\"2026-07-16T00:00:00Z\",\"i\":\"00000000-0000-0000-0000-000000000002\"}".getBytes())
                .when(objectMapper).writeValueAsBytes(any());

        CursorPage<XpTransactionResponse> result = gamificationService.getXpHistory(userId, null, 1);

        assertThat(result.getEdges()).hasSize(1);
        assertThat(result.isHasNextPage()).isTrue();
        assertThat(result.getEndCursor()).isNotNull();
    }

    @Test
    void getXpHistory_invalidCursor_throwsException() {
        UUID userId = UUID.randomUUID();
        assertThatThrownBy(() -> gamificationService.getXpHistory(userId, "!!!invalidbase64!!!", 10))
                .isInstanceOf(com.riksa.common.exception.InvalidCursorException.class);
    }

    @Test
    void getXpHistory_limitClampedToMax50() {
        UUID userId = UUID.randomUUID();
        when(xpTransactionRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        CursorPage<XpTransactionResponse> result = gamificationService.getXpHistory(userId, null, 1000);

        // Should not throw, just clamp
        assertThat(result.getEdges()).isEmpty();
    }

    @Test
    void getAchievements_returnsAllWithEarnedStatus() {
        UUID userId = UUID.randomUUID();
        Achievement a1 = new Achievement();
        a1.setId(UUID.randomUUID());
        a1.setName("First Lesson");
        a1.setDescription("Complete your first lesson");
        a1.setIconUrl("icon1.png");
        a1.setCriteriaType(AchievementCriteriaType.COMPLETE_N_COURSES);
        a1.setCriteriaValue(1L);

        Achievement a2 = new Achievement();
        a2.setId(UUID.randomUUID());
        a2.setName("Streak Master");
        a2.setDescription("Reach a 7-day streak");
        a2.setIconUrl("icon2.png");
        a2.setCriteriaType(AchievementCriteriaType.MAINTAIN_STREAK);
        a2.setCriteriaValue(7L);

        UserAchievement ua1 = new UserAchievement();
        ua1.setUserId(userId);
        ua1.setAchievementId(a1.getId());
        ua1.setUnlockedAt(OffsetDateTime.now());

        UserGamification ug = new UserGamification();
        ug.setUserId(userId);
        ug.setTotalXp(100);
        ug.setCurrentStreak(3);

        when(achievementRepository.findAll()).thenReturn(List.of(a1, a2));
        when(userAchievementRepository.findByUserId(userId)).thenReturn(List.of(ua1));
        when(userGamificationRepository.findByUserId(userId)).thenReturn(Optional.of(ug));
        when(lessonCompletionRepository.countByUserId(userId)).thenReturn(1L);

        List<AchievementResponse> result = gamificationService.getAchievements(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isEarned()).isTrue();
        assertThat(result.get(0).getUnlockedAt()).isNotNull();
        assertThat(result.get(1).isEarned()).isFalse();
        assertThat(result.get(1).getUnlockedAt()).isNull();
    }

    @Test
    void getRankHistory_returnsHistoryOrderedNewestFirst() {
        UUID userId = UUID.randomUUID();
        RankHistory h1 = new RankHistory();
        h1.setId(UUID.randomUUID());
        h1.setUserId(userId);
        h1.setScope(com.riksa.common.enums.ScopeType.COUNTRY);
        h1.setOldRank(TierType.BRONZE);
        h1.setNewRank(TierType.SILVER);
        h1.setChangedAt(OffsetDateTime.now());

        when(rankHistoryRepository.findByUserIdOrderByChangedAtDesc(eq(userId), any()))
                .thenReturn(List.of(h1));

        CursorPage<RankHistoryResponse> result = gamificationService.getRankHistory(userId, null, 10);

        assertThat(result.getEdges()).hasSize(1);
        assertThat(result.getEdges().get(0).getOldRank()).isEqualTo("BRONZE");
        assertThat(result.getEdges().get(0).getNewRank()).isEqualTo("SILVER");
    }

    @Test
    void getStreak_existingUser_returnsStreakInfo() {
        UUID userId = UUID.randomUUID();
        UserGamification ug = new UserGamification();
        ug.setUserId(userId);
        ug.setCurrentStreak(5);
        ug.setHighestStreak(15);

        StreakMilestone current = new StreakMilestone();
        current.setMinStreak(3);
        current.setStreakName("Getting Started");
        current.setIconUrl("start.png");

        StreakMilestone next = new StreakMilestone();
        next.setMinStreak(7);
        next.setStreakName("Week Warrior");
        next.setIconUrl("week.png");

        when(userGamificationRepository.findByUserId(userId)).thenReturn(Optional.of(ug));
        when(streakMilestoneRepository.findFirstByMinStreakLessThanEqualOrderByMinStreakDesc(5))
                .thenReturn(Optional.of(current));
        when(streakMilestoneRepository.findAll()).thenReturn(List.of(current, next));

        StreakResponse result = gamificationService.getStreak(userId);

        assertThat(result.getCurrentStreak()).isEqualTo(5);
        assertThat(result.getHighestStreak()).isEqualTo(15);
        assertThat(result.getCurrentMilestoneName()).isEqualTo("Getting Started");
        assertThat(result.getNextMilestoneName()).isEqualTo("Week Warrior");
        assertThat(result.getNextMilestoneMinStreak()).isEqualTo(7);
        assertThat(result.getDaysToNext()).isEqualTo(2);
    }

    @Test
    void getStreak_nonExistentUser_throwsException() {
        UUID userId = UUID.randomUUID();
        when(userGamificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gamificationService.getStreak(userId))
                .isInstanceOf(com.riksa.common.exception.ResourceNotFoundException.class);
    }

    @Test
    void getLeaderboard_returnsSortedEntries() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        UserGamification ug1 = new UserGamification();
        ug1.setUserId(userId1);
        ug1.setTotalXp(1000);

        UserGamification ug2 = new UserGamification();
        ug2.setUserId(userId2);
        ug2.setTotalXp(500);

        User user1 = new User();
        user1.setId(userId1);
        user1.setDisplayName("Alice");
        user1.setAvatarUrl("alice.png");

        User user2 = new User();
        user2.setId(userId2);
        user2.setDisplayName("Bob");
        user2.setAvatarUrl("bob.png");

        RankTier bronze = new RankTier();
        bronze.setTierName(TierType.BRONZE);
        bronze.setMinXp(0);
        bronze.setIconUrl("bronze.png");

        RankTier silver = new RankTier();
        silver.setTierName(TierType.SILVER);
        silver.setMinXp(1000);
        silver.setIconUrl("silver.png");

        when(userGamificationRepository.findAllByOrderByTotalXpDesc(any()))
                .thenReturn(List.of(ug1, ug2));
        when(userRepository.findAllById(any())).thenReturn(List.of(user1, user2));
        when(rankTierRepository.findAll()).thenReturn(List.of(bronze, silver));

        CursorPage<LeaderboardEntryResponse> result = gamificationService.getLeaderboard(null, 10);

        assertThat(result.getEdges()).hasSize(2);
        assertThat(result.getEdges().get(0).getRank()).isEqualTo(1);
        assertThat(result.getEdges().get(0).getDisplayName()).isEqualTo("Alice");
        assertThat(result.getEdges().get(0).getTotalXp()).isEqualTo(1000);
        assertThat(result.getEdges().get(0).getTierName()).isEqualTo("SILVER");
        assertThat(result.getEdges().get(1).getRank()).isEqualTo(2);
        assertThat(result.getEdges().get(1).getDisplayName()).isEqualTo("Bob");
        assertThat(result.getEdges().get(1).getTierName()).isEqualTo("BRONZE");
    }

    @Test
    void getLeaderboard_invalidCursor_throwsException() {
        assertThatThrownBy(() -> gamificationService.getLeaderboard("!!!invalid!!!", 10))
                .isInstanceOf(com.riksa.common.exception.InvalidCursorException.class);
    }

    @Test
    void getLeaderboard_userNotFound_usesUnknownDisplayName() {
        UUID userId1 = UUID.randomUUID();
        UserGamification ug1 = new UserGamification();
        ug1.setUserId(userId1);
        ug1.setTotalXp(100);

        when(userGamificationRepository.findAllByOrderByTotalXpDesc(any()))
                .thenReturn(List.of(ug1));
        when(userRepository.findAllById(any())).thenReturn(List.of());
        when(rankTierRepository.findAll()).thenReturn(List.of());

        CursorPage<LeaderboardEntryResponse> result = gamificationService.getLeaderboard(null, 10);

        assertThat(result.getEdges().get(0).getDisplayName()).isEqualTo("Deleted User");
        assertThat(result.getEdges().get(0).getAvatarUrl()).isNull();
    }
}
