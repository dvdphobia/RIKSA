package com.riksa.quest.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record WeekQuestsResponse(LocalDate weekStart, OffsetDateTime resetsAt,
        List<WeeklyQuestResponse> quests, QuestChestResponse chest) {}
