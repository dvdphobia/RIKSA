package com.riksa.quest.service;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.service.XpLedgerService;
import com.riksa.quest.dto.DailyQuestResponse;
import com.riksa.quest.dto.TodayQuestsResponse;
import com.riksa.quest.entity.DailyQuest;
import com.riksa.quest.entity.QuestType;
import com.riksa.quest.repository.DailyQuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class QuestService {
    public static final int ALL_CLAIMED_BONUS_XP = 30;
    public static final ZoneId DAY_ZONE = ZoneId.of("Asia/Phnom_Penh");
    private final DailyQuestRepository questRepository;
    private final XpLedgerService xpLedgerService;
    private final UserGamificationRepository userGamificationRepository;
    private final Clock clock;
    @Transactional
    public TodayQuestsResponse getToday(UUID userId) {
        LocalDate date = today();
        return new TodayQuestsResponse(date, ensureQuests(userId, date).stream().map(this::response).toList(),
                ALL_CLAIMED_BONUS_XP);
    }
    @Transactional public void recordLesson(UUID userId) { recordProgress(userId, QuestType.COMPLETE_LESSON); }
    @Transactional public void recordCorrectAnswer(UUID userId) { recordProgress(userId, QuestType.CORRECT_ANSWERS); }
    @Transactional public void recordSocialAction(UUID userId) { recordProgress(userId, QuestType.SOCIAL_ACTION); }
    @Transactional
    public DailyQuestResponse claim(UUID userId, UUID questId) {
        LocalDate date = today();
        // A per-user lock serializes simultaneous last claims, so the daily bonus is awarded once.
        if (userGamificationRepository.findByIdForUpdate(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quest not found");
        }
        DailyQuest quest = questRepository.findForUpdate(questId, userId, date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quest not found"));
        if (quest.isClaimed() || quest.getProgress() < quest.getTarget()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quest is not claimable");
        }
        quest.setClaimed(true);
        quest.setClaimedAt(OffsetDateTime.now(clock));
        questRepository.saveAndFlush(quest);
        xpLedgerService.awardXp(userId, quest.getXpReward(), "QUEST", quest.getId());
        if (questRepository.countByUserIdAndQuestDateAndClaimedFalse(userId, date) == 0) {
            xpLedgerService.awardXp(userId, ALL_CLAIMED_BONUS_XP, "QUEST_BONUS", bonusSourceId(userId, date));
        }
        return response(quest);
    }
    private void recordProgress(UUID userId, QuestType type) {
        LocalDate date = today();
        ensureQuests(userId, date);
        questRepository.incrementProgress(userId, date, type);
    }
    private List<DailyQuest> ensureQuests(UUID userId, LocalDate date) {
        for (QuestType type : QuestType.values()) {
            questRepository.insertIfAbsent(UUID.randomUUID(), userId, date, type.name(), type.target(), type.xpReward());
        }
        return questRepository.findByUserIdAndQuestDateOrderByTypeAsc(userId, date);
    }
    private DailyQuestResponse response(DailyQuest quest) {
        QuestType type = quest.getType();
        return new DailyQuestResponse(quest.getId(), type.name(), type.title(), type.titleKm(),
                quest.getTarget(), quest.getProgress(), quest.getXpReward(),
                quest.getProgress() >= quest.getTarget(), quest.isClaimed());
    }
    private LocalDate today() { return LocalDate.now(clock.withZone(DAY_ZONE)); }
    private UUID bonusSourceId(UUID userId, LocalDate date) {
        return UUID.nameUUIDFromBytes(("quest-bonus:" + userId + ":" + date).getBytes(StandardCharsets.UTF_8));
    }
}
