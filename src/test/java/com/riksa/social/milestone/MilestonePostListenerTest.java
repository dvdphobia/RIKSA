package com.riksa.social.milestone;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.enums.TierType;
import com.riksa.gamification.entity.StreakMilestone;
import com.riksa.gamification.event.RankChangedEvent;
import com.riksa.gamification.event.StreakUpdatedEvent;
import com.riksa.gamification.repository.StreakMilestoneRepository;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.entity.LessonCompletion;
import com.riksa.learning.entity.RoadmapUnit;
import com.riksa.learning.event.LessonCompletedEvent;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.social.entity.Category;
import com.riksa.social.repository.CategoryRepository;
import com.riksa.social.listener.MilestonePostListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilestonePostListenerTest {
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private StreakMilestoneRepository streakMilestoneRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonCompletionRepository lessonCompletionRepository;
    @InjectMocks private MilestonePostListener listener;

    private Category wins;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(USER_ID);
        user.setShareMilestones(true);
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        wins = new Category();
        wins.setId(UUID.fromString("50000000-0000-0000-0000-000000000001"));
        lenient().when(categoryRepository.findBySlug("wins")).thenReturn(Optional.of(wins));
    }

    @Test
    void createsPostWhenStreakReachesConfiguredMilestone() {
        StreakMilestone threshold = new StreakMilestone();
        threshold.setMinStreak(7);
        threshold.setStreakName("Week");
        when(streakMilestoneRepository.findFirstByMinStreakLessThanEqualOrderByMinStreakDesc(7))
                .thenReturn(Optional.of(threshold));

        listener.onStreakUpdated(new StreakUpdatedEvent(this, USER_ID, 7, 7));

        verifyInsert("STREAK", 7);
    }

    @Test
    void createsPostOnlyWhenRankMovesUp() {
        listener.onRankChanged(new RankChangedEvent(this, USER_ID, "BRONZE", "SILVER", "COUNTRY"));
        verifyInsert("RANK_UP", TierType.SILVER.ordinal() + 1);

        clearInvocations(jdbcTemplate, categoryRepository, userRepository);
        listener.onRankChanged(new RankChangedEvent(this, USER_ID, "GOLD", "SILVER", "COUNTRY"));
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void createsUnitPostOnlyAfterEveryLessonIsComplete() {
        UUID lessonId = UUID.fromString("60000000-0000-0000-0000-000000000001");
        UUID unitId = UUID.fromString("70000000-0000-0000-0000-000000000001");
        RoadmapUnit unit = new RoadmapUnit();
        unit.setId(unitId);
        unit.setTitle("Atoms");
        unit.setPosition(2);
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setRoadmapUnit(unit);
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findRoadmapLessons(List.of(unitId))).thenReturn(List.of(lesson));
        when(lessonCompletionRepository.findByUserIdAndLessonIdIn(USER_ID, List.of(lessonId)))
                .thenReturn(List.of(new LessonCompletion()));

        listener.onLessonCompleted(new LessonCompletedEvent(this, USER_ID, lessonId, 10));

        verifyInsert("UNIT_COMPLETE", unitId.hashCode() & Integer.MAX_VALUE);
    }

    @Test
    void optOutPreventsCreatingMilestonePosts() {
        User user = new User();
        user.setId(USER_ID);
        user.setShareMilestones(false);
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        listener.onRankChanged(new RankChangedEvent(this, USER_ID, "BRONZE", "SILVER", "COUNTRY"));

        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void insertUsesDatabaseUniqueConflictRuleForIdempotency() {
        listener.onRankChanged(new RankChangedEvent(this, USER_ID, "BRONZE", "SILVER", "COUNTRY"));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sql.capture(), any(Object[].class));
        assertTrue(sql.getValue().contains("ON CONFLICT"));
        assertTrue(sql.getValue().contains("milestone_value"));
    }

    private void verifyInsert(String type, int value) {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        String title = switch (type) {
            case "STREAK" -> value + "-day streak";
            case "RANK_UP" -> "Silver";
            case "UNIT_COMPLETE" -> "Atoms";
            default -> throw new AssertionError("Unexpected milestone type: " + type);
        };
        verify(jdbcTemplate).update(sql.capture(), eq(title), anyString(), eq(USER_ID),
                eq(wins.getId()), eq(type), eq(title), anyString(), eq(value));
        assertTrue(sql.getValue().contains("ON CONFLICT"));
        assertTrue(sql.getValue().contains("'MILESTONE'"));
        assertTrue(sql.getValue().contains("milestone_value"));
    }
}
