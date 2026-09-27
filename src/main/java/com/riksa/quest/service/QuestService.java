package com.riksa.quest.service;

import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.service.XpLedgerService;
import com.riksa.market.dto.ChestRewardItemResponse;
import com.riksa.market.service.MarketService;
import com.riksa.quest.dto.DailyQuestResponse;
import com.riksa.quest.dto.QuestChestResponse;
import com.riksa.quest.dto.QuestResponse;
import com.riksa.quest.dto.TodayQuestsResponse;
import com.riksa.quest.dto.WeekQuestsResponse;
import com.riksa.quest.dto.WeeklyQuestResponse;
import com.riksa.quest.entity.DailyQuest;
import com.riksa.quest.entity.QuestChest;
import com.riksa.quest.entity.QuestType;
import com.riksa.quest.entity.WeeklyQuest;
import com.riksa.quest.entity.WeeklyQuestType;
import com.riksa.quest.repository.DailyQuestRepository;
import com.riksa.quest.repository.QuestChestRepository;
import com.riksa.quest.repository.WeeklyQuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestService {
    public static final int ALL_CLAIMED_BONUS_XP = 30;
    public static final int CHEST_TARGET_POINTS = 15;
    public static final int CHEST_MAX_POINTS = 21;
    public static final int CHEST_XP_REWARD = 50;
    public static final ZoneId DAY_ZONE = ZoneId.of("Asia/Phnom_Penh");

    private final DailyQuestRepository questRepository;
    private final WeeklyQuestRepository weeklyQuestRepository;
    private final QuestChestRepository questChestRepository;
    private final XpLedgerService xpLedgerService;
    private final UserGamificationRepository userGamificationRepository;
    private final MarketService marketService;
    private final Clock clock;

    @Transactional
    public TodayQuestsResponse getToday(UUID userId) {
        LocalDate date = today();
        return new TodayQuestsResponse(date, resetsAt(date),
                ensureQuests(userId, date).stream().map(this::response).toList(), ALL_CLAIMED_BONUS_XP);
    }

    @Transactional
    public WeekQuestsResponse getWeek(UUID userId) {
        LocalDate weekStart = weekStart(today());
        List<WeeklyQuest> quests = ensureWeeklyQuests(userId, weekStart);
        QuestChest chest = ensureChest(userId, weekStart);
        return new WeekQuestsResponse(weekStart, resetsAt(weekStart.plusDays(6)),
                quests.stream().map(this::weeklyResponse).toList(), chestResponse(userId, weekStart, chest));
    }

    @Transactional
    public void recordLesson(UUID userId) {
        recordProgress(userId, QuestType.COMPLETE_LESSON, WeeklyQuestType.WEEKLY_LESSONS);
        LocalDate start = weekStart(today());
        weeklyQuestRepository.refreshActiveDaysProgress(userId, start, start.plusWeeks(1));
    }

    @Transactional
    public void recordCorrectAnswer(UUID userId) {
        recordProgress(userId, QuestType.CORRECT_ANSWERS, WeeklyQuestType.WEEKLY_CORRECT_ANSWERS);
    }

    @Transactional
    public void recordSocialAction(UUID userId) {
        recordProgress(userId, QuestType.SOCIAL_ACTION, WeeklyQuestType.WEEKLY_SOCIAL);
    }

    @Transactional
    public QuestResponse claim(UUID userId, UUID questId) {
        LocalDate date = today();
        LocalDate start = weekStart(date);
        // A per-user lock serializes simultaneous last daily claims, so the daily bonus is awarded once.
        if (userGamificationRepository.findByIdForUpdate(userId).isEmpty()) {
            throw notFound();
        }

        DailyQuest dailyQuest = questRepository.findForUpdate(questId, userId, date).orElse(null);
        if (dailyQuest != null) {
            return claimDaily(userId, dailyQuest, date);
        }

        WeeklyQuest weeklyQuest = weeklyQuestRepository.findForUpdate(questId, userId, start)
                .orElseThrow(QuestService::notFound);
        if (weeklyQuest.isClaimed() || weeklyQuest.getProgress() < weeklyQuest.getTarget()) {
            throw notClaimable();
        }
        weeklyQuest.setClaimed(true);
        weeklyQuest.setClaimedAt(OffsetDateTime.now(clock));
        weeklyQuestRepository.saveAndFlush(weeklyQuest);
        xpLedgerService.awardXp(userId, weeklyQuest.getXpReward(), "WEEKLY_QUEST", weeklyQuest.getId());
        return response(weeklyQuest);
    }

    @Transactional
    public QuestChestResponse openChest(UUID userId) {
        LocalDate start = weekStart(today());
        questChestRepository.insertIfAbsent(UUID.randomUUID(), userId, start);
        QuestChest chest = questChestRepository.findForUpdate(userId, start)
                .orElseThrow(QuestService::notFound);
        int points = chestPoints(userId, start);
        if (chest.getOpenedAt() != null || points < CHEST_TARGET_POINTS) {
            throw notClaimable();
        }
        chest.setOpenedAt(OffsetDateTime.now(clock));
        questChestRepository.saveAndFlush(chest);
        xpLedgerService.awardXp(userId, CHEST_XP_REWARD, "QUEST_CHEST", chest.getId());
        marketService.grantChestItem(userId);
        return chestResponse(userId, start, chest);
    }

    private QuestResponse claimDaily(UUID userId, DailyQuest quest, LocalDate date) {
        if (quest.isClaimed() || quest.getProgress() < quest.getTarget()) {
            throw notClaimable();
        }
        quest.setClaimed(true);
        quest.setClaimedAt(OffsetDateTime.now(clock));
        questRepository.saveAndFlush(quest);
        xpLedgerService.awardXp(userId, quest.getXpReward(), "QUEST", quest.getId());
        if (questRepository.countByUserIdAndQuestDateAndClaimedFalse(userId, date) == 0) {
            xpLedgerService.awardXp(userId, ALL_CLAIMED_BONUS_XP, "QUEST_BONUS", bonusSourceId(userId, date));
        }
        QuestType type = quest.getType();
        return new QuestResponse(quest.getId(), type.name(), type.title(), type.titleKm(),
                quest.getTarget(), quest.getProgress(), quest.getXpReward(),
                quest.getProgress() >= quest.getTarget(), quest.isClaimed());
    }

    private void recordProgress(UUID userId, QuestType dailyType, WeeklyQuestType weeklyType) {
        LocalDate date = today();
        LocalDate start = weekStart(date);
        ensureQuests(userId, date);
        questRepository.incrementProgress(userId, date, dailyType);
        ensureWeeklyQuests(userId, start);
        weeklyQuestRepository.incrementProgress(userId, start, weeklyType);
    }

    private List<DailyQuest> ensureQuests(UUID userId, LocalDate date) {
        for (QuestType type : QuestType.values()) {
            questRepository.insertIfAbsent(UUID.randomUUID(), userId, date, type.name(), type.target(), type.xpReward());
        }
        return questRepository.findByUserIdAndQuestDateOrderByTypeAsc(userId, date);
    }

    private List<WeeklyQuest> ensureWeeklyQuests(UUID userId, LocalDate start) {
        for (WeeklyQuestType type : WeeklyQuestType.values()) {
            weeklyQuestRepository.insertIfAbsent(UUID.randomUUID(), userId, start, type.name(),
                    type.target(), type.xpReward());
        }
        return weeklyQuestRepository.findByUserIdAndWeekStartOrderByTypeAsc(userId, start);
    }

    private QuestChest ensureChest(UUID userId, LocalDate start) {
        questChestRepository.insertIfAbsent(UUID.randomUUID(), userId, start);
        return questChestRepository.findByUserIdAndWeekStart(userId, start).orElseThrow(QuestService::notFound);
    }

    private DailyQuestResponse response(DailyQuest quest) {
        QuestType type = quest.getType();
        return new DailyQuestResponse(quest.getId(), type.name(), type.title(), type.titleKm(),
                quest.getTarget(), quest.getProgress(), quest.getXpReward(),
                quest.getProgress() >= quest.getTarget(), quest.isClaimed());
    }

    private QuestResponse response(WeeklyQuest quest) {
        WeeklyQuestType type = quest.getType();
        return new QuestResponse(quest.getId(), type.name(), type.title(), type.titleKm(),
                quest.getTarget(), quest.getProgress(), quest.getXpReward(),
                quest.getProgress() >= quest.getTarget(), quest.isClaimed());
    }

    private WeeklyQuestResponse weeklyResponse(WeeklyQuest quest) {
        WeeklyQuestType type = quest.getType();
        return new WeeklyQuestResponse(quest.getId(), type.name(), type.title(), type.titleKm(),
                quest.getTarget(), quest.getProgress(), quest.getXpReward(),
                quest.getProgress() >= quest.getTarget(), quest.isClaimed());
    }

    private QuestChestResponse chestResponse(UUID userId, LocalDate start, QuestChest chest) {
        ChestRewardItemResponse reward = chest.getOpenedAt() == null ? null : marketService.findChestReward(
                userId,
                start.atStartOfDay(DAY_ZONE).toOffsetDateTime(),
                start.plusWeeks(1).atStartOfDay(DAY_ZONE).toOffsetDateTime()).orElse(null);
        return new QuestChestResponse(chest.getId(), chestPoints(userId, start), CHEST_TARGET_POINTS,
                CHEST_MAX_POINTS, chest.getOpenedAt() != null, CHEST_XP_REWARD, reward);
    }

    private int chestPoints(UUID userId, LocalDate start) {
        long claimedDailyQuests = questRepository.countByUserIdAndQuestDateGreaterThanEqualAndQuestDateLessThanAndClaimedTrue(
                userId, start, start.plusWeeks(1));
        return (int) Math.min(CHEST_MAX_POINTS, claimedDailyQuests);
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(DAY_ZONE));
    }

    private LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private OffsetDateTime resetsAt(LocalDate finalDate) {
        return finalDate.plusDays(1).atStartOfDay(DAY_ZONE).toOffsetDateTime();
    }

    private UUID bonusSourceId(UUID userId, LocalDate date) {
        return UUID.nameUUIDFromBytes(("quest-bonus:" + userId + ":" + date).getBytes(StandardCharsets.UTF_8));
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Quest not found");
    }

    private static ResponseStatusException notClaimable() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Quest is not claimable");
    }
}
