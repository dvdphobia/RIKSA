package com.riksa.quest.dto;
import java.time.LocalDate;
import java.util.List;
public record TodayQuestsResponse(LocalDate date, List<DailyQuestResponse> quests, int allClaimedBonusXp) {}
