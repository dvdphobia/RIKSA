package com.riksa.learning.service;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.enums.DifficultyLevel;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.repository.XpTransactionRepository;
import com.riksa.gamification.service.StreakService;
import com.riksa.learning.dto.CompletionRequest;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/** Integration check that a synchronous reward listener failure rolls back the completion and XP ledger. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RIKSA_RUN_INTEGRATION_TESTS", matches = "true")
class LessonCompletionAtomicityIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired UserGamificationRepository gamificationRepository;
    @Autowired LessonRepository lessonRepository;
    @Autowired LessonCompletionRepository completionRepository;
    @Autowired XpTransactionRepository xpTransactionRepository;
    @Autowired LessonCompletionService completionService;

    @MockitoSpyBean StreakService streakService;

    @Test
    void beforeCommitStreakFailureRollsBackCompletionAndXp() {
        User user = new User();
        user.setUsername("completion-rollback-" + UUID.randomUUID());
        user.setDisplayName("Completion Rollback User");
        user.setEmail(UUID.randomUUID() + "@example.test");
        user = userRepository.saveAndFlush(user);

        UserGamification gamification = new UserGamification();
        gamification.setUserId(user.getId());
        gamificationRepository.saveAndFlush(gamification);

        Lesson lesson = new Lesson();
        lesson.setTitle("Rollback integration lesson");
        lesson.setDifficulty(DifficultyLevel.EASY);
        lesson.setBaseXp(30);
        lesson = lessonRepository.saveAndFlush(lesson);

        UUID userId = user.getId();
        UUID lessonId = lesson.getId();
        try {
            doThrow(new IllegalStateException("simulated streak listener failure"))
                    .when(streakService).updateStreak(eq(userId), any(OffsetDateTime.class));

            CompletionRequest request = new CompletionRequest();
            request.setReflectionText("I learned how a failed reward listener rolls the entire completion back today");
            assertThatThrownBy(() -> completionService.complete(userId, lessonId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("simulated streak listener failure");

            assertThat(completionRepository.findByUserIdAndLessonId(userId, lessonId)).isEmpty();
            assertThat(xpTransactionRepository.findByUserIdAndSourceTypeAndSourceId(
                    userId, "LESSON_COMPLETION", lessonId)).isEmpty();
            assertThat(gamificationRepository.findByUserId(userId).orElseThrow().getTotalXp()).isZero();
            verify(streakService).updateStreak(eq(userId), any(OffsetDateTime.class));
        } finally {
            lessonRepository.deleteById(lessonId);
            userRepository.deleteById(userId);
        }
    }
}
