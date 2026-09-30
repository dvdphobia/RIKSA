package com.riksa.gamification.service;

import com.riksa.gamification.entity.Achievement;
import com.riksa.gamification.entity.XpTransaction;
import com.riksa.gamification.repository.AchievementRepository;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.quest.entity.DailyQuest;
import com.riksa.quest.entity.WeeklyQuest;
import com.riksa.quest.repository.DailyQuestRepository;
import com.riksa.quest.repository.WeeklyQuestRepository;
import com.riksa.quest.setup.SetupTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Names what an XP grant was for — the lesson, achievement or quest — so the
 * progress history can say more than "Lesson Completion". One query per
 * source type for the whole page.
 */
@Component
@RequiredArgsConstructor
public class XpDetailResolver {

    public record Detail(String text, String textKm) {}

    private final LessonRepository lessonRepository;
    private final AchievementRepository achievementRepository;
    private final DailyQuestRepository dailyQuestRepository;
    private final WeeklyQuestRepository weeklyQuestRepository;

    public Map<UUID, Detail> resolve(List<XpTransaction> rows, UUID userId) {
        Map<UUID, Detail> details = new HashMap<>();

        Map<UUID, Lesson> lessons = lessonRepository.findAllById(ids(rows, "LESSON_COMPLETION")).stream()
                .collect(Collectors.toMap(Lesson::getId, lesson -> lesson));
        Map<UUID, Achievement> achievements = achievementRepository.findAllById(ids(rows, "ACHIEVEMENT_UNLOCK"))
                .stream().collect(Collectors.toMap(Achievement::getId, achievement -> achievement));
        Map<UUID, DailyQuest> daily = dailyQuestRepository.findAllById(ids(rows, "QUEST")).stream()
                .collect(Collectors.toMap(DailyQuest::getId, quest -> quest));
        Map<UUID, WeeklyQuest> weekly = weeklyQuestRepository.findAllById(ids(rows, "WEEKLY_QUEST")).stream()
                .collect(Collectors.toMap(WeeklyQuest::getId, quest -> quest));
        Map<UUID, SetupTask> setup = new HashMap<>();
        for (SetupTask task : SetupTask.values()) setup.put(task.sourceId(userId), task);

        for (XpTransaction row : rows) {
            UUID id = row.getSourceId();
            if (id == null) continue;
            Detail detail = switch (row.getSourceType()) {
                case "LESSON_COMPLETION" -> {
                    Lesson lesson = lessons.get(id);
                    yield lesson == null ? null : new Detail(lesson.getTitle(), null);
                }
                case "ACHIEVEMENT_UNLOCK" -> {
                    Achievement achievement = achievements.get(id);
                    yield achievement == null ? null : new Detail(achievement.getName(), null);
                }
                case "QUEST" -> {
                    DailyQuest quest = daily.get(id);
                    yield quest == null ? null : new Detail(quest.getType().title(), quest.getType().titleKm());
                }
                case "WEEKLY_QUEST" -> {
                    WeeklyQuest quest = weekly.get(id);
                    yield quest == null ? null : new Detail(quest.getType().title(), quest.getType().titleKm());
                }
                case SetupTask.XP_SOURCE -> {
                    SetupTask task = setup.get(id);
                    yield task == null ? null : new Detail(task.title(), task.titleKm());
                }
                default -> null;
            };
            if (detail != null) details.put(row.getId(), detail);
        }
        return details;
    }

    private static Set<UUID> ids(Collection<XpTransaction> rows, String sourceType) {
        return rows.stream()
                .filter(row -> sourceType.equals(row.getSourceType()))
                .map(XpTransaction::getSourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
