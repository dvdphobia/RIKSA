package com.riksa.social.listener;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.enums.TierType;
import com.riksa.gamification.entity.StreakMilestone;
import com.riksa.gamification.event.RankChangedEvent;
import com.riksa.gamification.event.StreakUpdatedEvent;
import com.riksa.gamification.repository.StreakMilestoneRepository;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.event.LessonCompletedEvent;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.social.entity.Category;
import com.riksa.social.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MilestonePostListener {
    private static final String WINS_CATEGORY = "wins";

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StreakMilestoneRepository streakMilestoneRepository;
    private final LessonRepository lessonRepository;
    private final LessonCompletionRepository lessonCompletionRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStreakUpdated(StreakUpdatedEvent event) {
        if (!canShare(event.getUserId())) return;
        StreakMilestone milestone = streakMilestoneRepository
                .findFirstByMinStreakLessThanEqualOrderByMinStreakDesc(event.getCurrentStreak())
                .filter(value -> value.getMinStreak() == event.getCurrentStreak())
                .orElse(null);
        if (milestone == null) return;

        int days = Math.toIntExact(event.getCurrentStreak());
        insert(event.getUserId(), "STREAK", days,
                days + "-day streak",
                days + " ថ្ងៃជាប់គ្នា",
                "Reached a " + days + "-day learning streak.",
                "បានរៀនជាប់គ្នា " + days + " ថ្ងៃ។");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRankChanged(RankChangedEvent event) {
        if (!canShare(event.getUserId())) return;
        TierType newTier;
        try {
            newTier = TierType.valueOf(event.getNewRank());
            TierType oldTier = TierType.valueOf(event.getOldRank());
            if (newTier.ordinal() <= oldTier.ordinal()) return;
        } catch (RuntimeException invalidTier) {
            return;
        }
        int rankValue = newTier.ordinal() + 1;
        insert(event.getUserId(), "RANK_UP", rankValue,
                titleCase(newTier.name()),
                "បានឡើងដល់ថ្នាក់ " + titleCase(newTier.name()),
                "Ranked up from " + event.getOldRank() + " to " + event.getNewRank() + ".",
                "បានឡើងពី " + event.getOldRank() + " ទៅ " + event.getNewRank() + "។");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLessonCompleted(LessonCompletedEvent event) {
        if (!canShare(event.getUserId())) return;
        Lesson lesson = lessonRepository.findById(event.getLessonId()).orElse(null);
        if (lesson == null || lesson.getRoadmapUnit() == null) return;

        UUID unitId = lesson.getRoadmapUnit().getId();
        List<Lesson> unitLessons = lessonRepository.findRoadmapLessons(List.of(unitId));
        if (unitLessons.isEmpty()) return;
        List<UUID> lessonIds = unitLessons.stream().map(Lesson::getId).toList();
        long completed = lessonCompletionRepository
                .findByUserIdAndLessonIdIn(event.getUserId(), lessonIds).size();
        if (completed < lessonIds.size()) return;

        String unitTitle = lesson.getRoadmapUnit().getTitle();
        int value = unitMilestoneValue(unitId);
        insert(event.getUserId(), "UNIT_COMPLETE", value,
                unitTitle,
                "បានបញ្ចប់ " + unitTitle,
                "Finished the " + unitTitle + " learning unit.",
                "បានបញ្ចប់ឯកតាមេរៀន " + unitTitle + "។");
    }

    private int unitMilestoneValue(UUID unitId) {
        return unitId.hashCode() & Integer.MAX_VALUE;
    }

    private boolean canShare(UUID userId) {
        return userRepository.findById(userId)
                .map(User::isShareMilestones)
                .orElse(false);
    }

    private void insert(
            UUID authorId,
            String type,
            int value,
            String title,
            String titleKm,
            String content,
            String contentKm) {
        Category category = categoryRepository.findBySlug(WINS_CATEGORY)
                .orElseThrow(() -> new IllegalStateException("Wins category is missing"));
        jdbcTemplate.update("""
                INSERT INTO posts (
                    id, title, content, reaction_count, comment_count, share_count,
                    author_id, category_id, status, kind, milestone_type,
                    milestone_title, milestone_title_km, milestone_value,
                    created_at, updated_at
                ) VALUES (
                    uuid_generate_v4(), ?, ?, 0, 0, 0,
                    ?, ?, 'VISIBLE', 'MILESTONE', ?, ?, ?, ?, now(), now()
                )
                ON CONFLICT (author_id, milestone_type, milestone_value)
                    WHERE kind = 'MILESTONE'
                DO NOTHING
                """, title, content + "\n\n" + contentKm, authorId, category.getId(),
                type, title, titleKm, value);
    }

    private String titleCase(String value) {
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
