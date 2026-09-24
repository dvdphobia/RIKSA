package com.riksa.quest.service;

import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.service.XpLedgerService;
import com.riksa.quest.dto.DailyQuestResponse;
import com.riksa.quest.entity.DailyQuest;
import com.riksa.quest.entity.QuestType;
import com.riksa.quest.repository.DailyQuestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class QuestServiceTest {
    private static final UUID USER_ID = UUID.fromString("3eb0153c-7e8c-4c93-85f2-169093a328df");
    private static final UUID QUEST_ID = UUID.fromString("807e046d-4bbf-4c21-9b65-a7eeb4dafb2f");
    private DailyQuestRepository questRepository;
    private XpLedgerService xpLedgerService;
    private UserGamificationRepository userGamificationRepository;
    private QuestService service;

    @BeforeEach
    void setUp() {
        questRepository = mock(DailyQuestRepository.class);
        xpLedgerService = mock(XpLedgerService.class);
        userGamificationRepository = mock(UserGamificationRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-23T18:30:00Z"), ZoneOffset.UTC);
        service = new QuestService(questRepository, xpLedgerService, userGamificationRepository, clock);
    }

    @Test
    void getTodayCreatesThreeQuestsInPhnomPenhDay() {
        DailyQuest lesson = quest(QuestType.COMPLETE_LESSON, 1, 0, false);
        DailyQuest answers = quest(QuestType.CORRECT_ANSWERS, 3, 2, false);
        DailyQuest social = quest(QuestType.SOCIAL_ACTION, 1, 1, true);
        when(questRepository.findByUserIdAndQuestDateOrderByTypeAsc(eq(USER_ID), any()))
                .thenReturn(List.of(lesson, answers, social));

        var response = service.getToday(USER_ID);

        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(response.quests()).hasSize(3);
        assertThat(response.allClaimedBonusXp()).isEqualTo(30);
        assertThat(response.quests().get(2).completed()).isTrue();
        assertThat(response.quests().get(2).titleKm()).isNotBlank();
        verify(questRepository, times(3)).insertIfAbsent(any(), eq(USER_ID), eq(LocalDate.of(2026, 9, 24)),
                any(), anyInt(), anyInt());
    }

    @Test
    void correctAnswerProgressCreatesDayBeforeFirstReadAndTargetsCorrectQuest() {
        service.recordCorrectAnswer(USER_ID);

        verify(questRepository).insertIfAbsent(any(), eq(USER_ID), eq(LocalDate.of(2026, 9, 24)),
                eq("CORRECT_ANSWERS"), eq(3), eq(15));
        verify(questRepository).incrementProgress(USER_ID, LocalDate.of(2026, 9, 24),
                QuestType.CORRECT_ANSWERS);
    }

    @Test
    void claimAwardsQuestAndFinalDailyBonus() {
        DailyQuest quest = quest(QuestType.COMPLETE_LESSON, 1, 1, false);
        when(userGamificationRepository.findByIdForUpdate(USER_ID))
                .thenReturn(Optional.of(mock(UserGamification.class)));
        when(questRepository.findForUpdate(QUEST_ID, USER_ID, LocalDate.of(2026, 9, 24)))
                .thenReturn(Optional.of(quest));
        when(questRepository.countByUserIdAndQuestDateAndClaimedFalse(USER_ID, LocalDate.of(2026, 9, 24)))
                .thenReturn(0L);

        DailyQuestResponse response = service.claim(USER_ID, QUEST_ID);

        assertThat(response.claimed()).isTrue();
        verify(questRepository).saveAndFlush(quest);
        verify(xpLedgerService).awardXp(USER_ID, 20, "QUEST", QUEST_ID);
        verify(xpLedgerService).awardXp(eq(USER_ID), eq(30), eq("QUEST_BONUS"), any(UUID.class));
    }

    @Test
    void claimReturnsConflictWhenIncomplete() {
        DailyQuest quest = quest(QuestType.COMPLETE_LESSON, 1, 0, false);
        when(userGamificationRepository.findByIdForUpdate(USER_ID))
                .thenReturn(Optional.of(mock(UserGamification.class)));
        when(questRepository.findForUpdate(QUEST_ID, USER_ID, LocalDate.of(2026, 9, 24)))
                .thenReturn(Optional.of(quest));

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

    private DailyQuest quest(QuestType type, int target, int progress, boolean claimed) {
        DailyQuest quest = new DailyQuest();
        quest.setId(QUEST_ID);
        quest.setUserId(USER_ID);
        quest.setQuestDate(LocalDate.of(2026, 9, 24));
        quest.setType(type);
        quest.setTarget(target);
        quest.setProgress(progress);
        quest.setXpReward(type.xpReward());
        quest.setClaimed(claimed);
        return quest;
    }
}
