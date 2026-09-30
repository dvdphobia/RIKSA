package com.riksa.quest.dto;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
public record TodayQuestsResponse(LocalDate date, OffsetDateTime resetsAt,
        List<DailyQuestResponse> quests, int allClaimedBonusXp) {}
