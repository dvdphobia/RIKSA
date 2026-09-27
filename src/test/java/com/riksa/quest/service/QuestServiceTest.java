package com.riksa.quest.service;

import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.service.XpLedgerService;
import com.riksa.market.dto.ChestRewardItemResponse;
import com.riksa.market.entity.MarketSlot;
import com.riksa.market.service.MarketService;
import com.riksa.quest.dto.QuestResponse;
import com.riksa.quest.entity.DailyQuest;
import com.riksa.quest.entity.QuestChest;
import com.riksa.quest.entity.QuestType;
import com.riksa.quest.entity.WeeklyQuest;
import com.riksa.quest.entity.WeeklyQuestType;
import com.riksa.quest.repository.DailyQuestRepository;
import com.riksa.quest.repository.QuestChestRepository;
import com.riksa.quest.repository.WeeklyQuestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QuestServiceTest {
    private static final UUID USER_ID = UUID.fromString("3eb0153c-7e8c-4c93-85f2-169093a328df");
    private static final UUID QUEST_ID = UUID.fromString("807e046d-4bbf-4c21-9b65-a7eeb4dafb2f");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
    private static final LocalDate WEEK_START = LocalDate.of(2026, 9, 21);

    private DailyQuestRepository questRepository;
    private WeeklyQuestRepository weeklyQuestRepository;
    private QuestChestRepository questChestRepository;
    private XpLedgerService xpLedgerService;
    private UserGamificationRepository userGamificationRepository;
    private MarketService marketService;
    private QuestService service;

    @BeforeEach
    void setUp() {
        questRepository = mock(DailyQuestRepository.class);
        weeklyQuestRepository = mock(WeeklyQuestRepository.class);
        questChestRepository = mock(QuestChestRepository.class);
        xpLedgerService = mock(XpLedgerService.class);
        userGamificationRepository = mock(UserGamificationRepository.class);
        marketService = mock(MarketService.class);
        service = newService(Instant.parse("2026-09-23T18:30:00Z"));
    }

    @Test
    void getTodayCreatesThreeQuestsInPhnomPenhDayAndIncludesMidnightReset() {
        DailyQuest lesson = dailyQuest(QuestType.COMPLETE_LESSON, 1, 0, false);
        DailyQuest answers = dailyQuest(QuestType.CORRECT_ANSWERS, 3, 2, false);
        DailyQuest social = dailyQuest(QuestType.SOCIAL_ACTION, 1, 1, true);
        when(questRepository.findByUserIdAndQuestDateOrderByTypeAsc(eq(USER_ID), any()))
                .thenReturn(List.of(lesson, answers, social));

        var response = service.getToday(USER_ID);

        assertThat(response.date()).isEqualTo(TODAY);
        assertThat(response.resetsAt()).isEqualTo(OffsetDateTime.parse("2026-09-25T00:00:00+07:00"));
        assertThat(response.quests()).hasSize(3);
        assertThat(response.allClaimedBonusXp()).isEqualTo(30);
        assertThat(response.quests().get(2).completed()).isTrue();
        assertThat(response.quests().get(2).titleKm()).isNotBlank();
        verify(questRepository, times(3)).insertIfAbsent(any(), eq(USER_ID), eq(TODAY), any(), anyInt(), anyInt());
    }

    @Test
    void getWeekLazilyCreatesFourQuestsAndChest() {
        when(weeklyQuestRepository.findByUserIdAndWeekStartOrderByTypeAsc(USER_ID, WEEK_START))
                .thenReturn(weeklyQuests());
        when(questChestRepository.findByUserIdAndWeekStart(USER_ID, WEEK_START)).thenReturn(Optional.of(chest()));

        var response = service.getWeek(USER_ID);

        assertThat(response.weekStart()).isEqualTo(WEEK_START);
        assertThat(response.resetsAt()).isEqualTo(OffsetDateTime.parse("2026-09-28T00:00:00+07:00"));
        assertThat(response.quests()).hasSize(4);
        assertThat(response.chest().id()).isEqualTo(QUEST_ID);
        assertThat(response.chest().points()).isZero();
        assertThat(response.chest().target()).isEqualTo(15);
        assertThat(response.chest().maxPoints()).isEqualTo(21);
        assertThat(response.chest().rewardItem()).isNull();
        verify(weeklyQuestRepository, times(4)).insertIfAbsent(any(), eq(USER_ID), eq(WEEK_START),
                any(), anyInt(), anyInt());
        verify(questChestRepository).insertIfAbsent(any(), eq(USER_ID), eq(WEEK_START));
    }

    @Test
    void weekBoundaryUsesPhnomPenhMondayMidnight() {
        when(questChestRepository.findByUserIdAndWeekStart(eq(USER_ID), any())).thenReturn(Optional.of(chest()));
        QuestService sundayService = newService(Instant.parse("2026-09-27T16:59:00Z")); // Sunday 23:59
        QuestService mondayService = newService(Instant.parse("2026-09-27T17:00:00Z")); // Monday 00:00

        assertThat(sundayService.getWeek(USER_ID).weekStart()).isEqualTo(LocalDate.of(2026, 9, 21));
        assertThat(mondayService.getWeek(USER_ID).weekStart()).isEqualTo(LocalDate.of(2026, 9, 28));
        verify(weeklyQuestRepository).insertIfAbsent(any(), eq(USER_ID), eq(LocalDate.of(2026, 9, 21)),
                eq("WEEKLY_LESSONS"), eq(7), eq(100));
        verify(weeklyQuestRepository).insertIfAbsent(any(), eq(USER_ID), eq(LocalDate.of(2026, 9, 28)),
                eq("WEEKLY_LESSONS"), eq(7), eq(100));
    }

    @Test
    void correctAnswersAndSocialActionsIncrementTheirDailyAndWeeklyQuests() {
        service.recordCorrectAnswer(USER_ID);
        service.recordSocialAction(USER_ID);

        verify(questRepository).incrementProgress(USER_ID, TODAY, QuestType.CORRECT_ANSWERS);
        verify(weeklyQuestRepository).incrementProgress(USER_ID, WEEK_START,
                WeeklyQuestType.WEEKLY_CORRECT_ANSWERS);
        verify(questRepository).incrementProgress(USER_ID, TODAY, QuestType.SOCIAL_ACTION);
        verify(weeklyQuestRepository).incrementProgress(USER_ID, WEEK_START, WeeklyQuestType.WEEKLY_SOCIAL);
    }

    @Test
    void lessonProgressUpdatesWeeklyLessonQuestAndRecomputesDistinctActiveDays() {
        service.recordLesson(USER_ID);
        service.recordLesson(USER_ID);

        verify(questRepository, times(2)).incrementProgress(USER_ID, TODAY, QuestType.COMPLETE_LESSON);
        verify(weeklyQuestRepository, times(2)).incrementProgress(USER_ID, WEEK_START,
                WeeklyQuestType.WEEKLY_LESSONS);
        // Each call assigns the number of distinct local completion dates from lesson_completions;
        // replaying today's event therefore leaves active-day progress unchanged in the database.
        verify(weeklyQuestRepository, times(2)).refreshActiveDaysProgress(USER_ID, WEEK_START,
                WEEK_START.plusWeeks(1));
    }

    @Test
    void claimAwardsDailyQuestAndFinalDailyBonus() {
        DailyQuest quest = dailyQuest(QuestType.COMPLETE_LESSON, 1, 1, false);
        when(userGamificationRepository.findByIdForUpdate(USER_ID))
                .thenReturn(Optional.of(mock(UserGamification.class)));
        when(questRepository.findForUpdate(QUEST_ID, USER_ID, TODAY)).thenReturn(Optional.of(quest));
        when(questRepository.countByUserIdAndQuestDateAndClaimedFalse(USER_ID, TODAY)).thenReturn(0L);

        QuestResponse response = service.claim(USER_ID, QUEST_ID);

        assertThat(response.claimed()).isTrue();
        verify(questRepository).saveAndFlush(quest);
        verify(xpLedgerService).awardXp(USER_ID, 20, "QUEST", QUEST_ID);
        verify(xpLedgerService).awardXp(eq(USER_ID), eq(30), eq("QUEST_BONUS"), any(UUID.class));
    }

    @Test
    void claimAwardsWeeklyQuestOnceAndRepeatReturnsConflict() {
        WeeklyQuest quest = weeklyQuest(WeeklyQuestType.WEEKLY_LESSONS, 7, 7, false);
        when(userGamificationRepository.findByIdForUpdate(USER_ID))
                .thenReturn(Optional.of(mock(UserGamification.class)));
        when(weeklyQuestRepository.findForUpdate(QUEST_ID, USER_ID, WEEK_START)).thenReturn(Optional.of(quest));

        QuestResponse response = service.claim(USER_ID, QUEST_ID);
        assertThat(response.type()).isEqualTo("WEEKLY_LESSONS");
        assertThat(response.claimed()).isTrue();
        assertThatThrownBy(() -> service.claim(USER_ID, QUEST_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(weeklyQuestRepository).saveAndFlush(quest);
        verify(xpLedgerService).awardXp(USER_ID, 100, "WEEKLY_QUEST", QUEST_ID);
    }

    @Test
    void chestPointsCountClaimedDailyQuestsInCurrentWeek() {
        when(questChestRepository.findByUserIdAndWeekStart(USER_ID, WEEK_START)).thenReturn(Optional.of(chest()));
        when(questRepository.countByUserIdAndQuestDateGreaterThanEqualAndQuestDateLessThanAndClaimedTrue(
                USER_ID, WEEK_START, WEEK_START.plusWeeks(1))).thenReturn(9L);

        var response = service.getWeek(USER_ID).chest();

        assertThat(response.points()).isEqualTo(9);
        verify(questRepository).countByUserIdAndQuestDateGreaterThanEqualAndQuestDateLessThanAndClaimedTrue(
                USER_ID, WEEK_START, WEEK_START.plusWeeks(1));
    }

    @Test
    void chestPointsAreCappedAtTwentyOne() {
        when(questChestRepository.findByUserIdAndWeekStart(USER_ID, WEEK_START)).thenReturn(Optional.of(chest()));
        when(questRepository.countByUserIdAndQuestDateGreaterThanEqualAndQuestDateLessThanAndClaimedTrue(
                USER_ID, WEEK_START, WEEK_START.plusWeeks(1))).thenReturn(24L);

        assertThat(service.getWeek(USER_ID).chest().points()).isEqualTo(21);
    }

    @Test
    void openingChestAtFifteenAwardsOnceAndSecondOpenConflicts() {
        QuestChest chest = chest();
        when(questChestRepository.findForUpdate(USER_ID, WEEK_START)).thenReturn(Optional.of(chest));
        when(questRepository.countByUserIdAndQuestDateGreaterThanEqualAndQuestDateLessThanAndClaimedTrue(
                USER_ID, WEEK_START, WEEK_START.plusWeeks(1))).thenReturn(15L);

        var response = service.openChest(USER_ID);
        assertThat(response.opened()).isTrue();
        assertThat(response.points()).isEqualTo(15);
        assertThat(response.rewardItem()).isNull();
        assertThatThrownBy(() -> service.openChest(USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(questChestRepository).saveAndFlush(chest);
        verify(xpLedgerService).awardXp(USER_ID, 50, "QUEST_CHEST", QUEST_ID);
    }

    @Test
    void openingChestGrantsTheMarketRewardItem() {
        QuestChest chest = chest();
        ChestRewardItemResponse reward = new ChestRewardItemResponse(
                UUID.randomUUID(), MarketSlot.HAT, "hat_straw", "Straw hat", "មួកចំបើង");
        when(questChestRepository.findForUpdate(USER_ID, WEEK_START)).thenReturn(Optional.of(chest));
        when(questRepository.countByUserIdAndQuestDateGreaterThanEqualAndQuestDateLessThanAndClaimedTrue(
                USER_ID, WEEK_START, WEEK_START.plusWeeks(1))).thenReturn(15L);
        when(marketService.grantChestItem(USER_ID)).thenReturn(Optional.of(reward));
        when(marketService.findChestReward(eq(USER_ID), any(), any())).thenReturn(Optional.of(reward));

        var response = service.openChest(USER_ID);

        assertThat(response.rewardItem()).isEqualTo(reward);
        verify(marketService).grantChestItem(USER_ID);
    }

    @Test
    void openingChestBelowFifteenConflictsWithoutAwardingXp() {
        QuestChest chest = chest();
        when(questChestRepository.findForUpdate(USER_ID, WEEK_START)).thenReturn(Optional.of(chest));
        when(questRepository.countByUserIdAndQuestDateGreaterThanEqualAndQuestDateLessThanAndClaimedTrue(
                USER_ID, WEEK_START, WEEK_START.plusWeeks(1))).thenReturn(14L);

        assertThatThrownBy(() -> service.openChest(USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
        verify(questChestRepository, never()).saveAndFlush(any());
        verifyNoInteractions(xpLedgerService);
    }

    @Test
    void claimReturnsConflictWhenDailyQuestIncomplete() {
        DailyQuest quest = dailyQuest(QuestType.COMPLETE_LESSON, 1, 0, false);
        when(userGamificationRepository.findByIdForUpdate(USER_ID))
                .thenReturn(Optional.of(mock(UserGamification.class)));
        when(questRepository.findForUpdate(QUEST_ID, USER_ID, TODAY)).thenReturn(Optional.of(quest));

        assertThatThrownBy(() -> service.claim(USER_ID, QUEST_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
        verifyNoInteractions(xpLedgerService);
    }

    @Test
    void claimReturnsNotFoundForAnotherLearnersQuest() {
        when(userGamificationRepository.findByIdForUpdate(USER_ID))
                .thenReturn(Optional.of(mock(UserGamification.class)));

        assertThatThrownBy(() -> service.claim(USER_ID, QUEST_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        verifyNoInteractions(xpLedgerService);
    }

    private QuestService newService(Instant instant) {
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
        return new QuestService(questRepository, weeklyQuestRepository, questChestRepository,
                xpLedgerService, userGamificationRepository, marketService, clock);
    }

    private DailyQuest dailyQuest(QuestType type, int target, int progress, boolean claimed) {
        DailyQuest quest = new DailyQuest();
        quest.setId(QUEST_ID);
        quest.setUserId(USER_ID);
        quest.setQuestDate(TODAY);
        quest.setType(type);
        quest.setTarget(target);
        quest.setProgress(progress);
        quest.setXpReward(type.xpReward());
        quest.setClaimed(claimed);
        return quest;
    }

    private WeeklyQuest weeklyQuest(WeeklyQuestType type, int target, int progress, boolean claimed) {
        WeeklyQuest quest = new WeeklyQuest();
        quest.setId(QUEST_ID);
        quest.setUserId(USER_ID);
        quest.setWeekStart(WEEK_START);
        quest.setType(type);
        quest.setTarget(target);
        quest.setProgress(progress);
        quest.setXpReward(type.xpReward());
        quest.setClaimed(claimed);
        return quest;
    }

    private List<WeeklyQuest> weeklyQuests() {
        return Arrays.stream(WeeklyQuestType.values())
                .map(type -> weeklyQuest(type, type.target(), 0, false))
                .toList();
    }

    private QuestChest chest() {
        QuestChest chest = new QuestChest();
        chest.setId(QUEST_ID);
        chest.setUserId(USER_ID);
        chest.setWeekStart(WEEK_START);
        return chest;
    }
}
