package com.riksa.learning.service;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.enums.DifficultyLevel;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.repository.RankHistoryRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.repository.XpTransactionRepository;
import com.riksa.learning.dto.CompletionRequest;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.ExerciseAttempt;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.repository.ExerciseAttemptRepository;
import com.riksa.learning.repository.ExerciseRepository;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.leaderboard.repository.LeaderboardSnapshotRepository;
import com.riksa.leaderboard.service.LeaderboardSnapshotService;
import com.riksa.quest.entity.DailyQuest;
import com.riksa.quest.entity.QuestType;
import com.riksa.quest.repository.DailyQuestRepository;
import com.riksa.quest.service.QuestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RIKSA_RUN_INTEGRATION_TESTS", matches = "true")
class LessonHotPathIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired UserGamificationRepository gamificationRepository;
    @Autowired LessonRepository lessonRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired ExerciseAttemptRepository attemptRepository;
    @Autowired LessonCompletionService completionService;
    @Autowired LessonCompletionRepository completionRepository;
    @Autowired XpTransactionRepository xpTransactionRepository;
    @Autowired DailyQuestRepository dailyQuestRepository;
    @Autowired RankHistoryRepository rankHistoryRepository;
    @Autowired LeaderboardSnapshotService leaderboardSnapshotService;
    @Autowired LeaderboardSnapshotRepository leaderboardSnapshotRepository;

    @Test
    void completingLessonDrivesXpStreakAndRankPipeline() throws Exception {
        User user = new User();
        user.setUsername("hotpath-" + UUID.randomUUID());
        user.setDisplayName("Hot Path User");
        user.setEmail(UUID.randomUUID() + "@example.test");
        user = userRepository.save(user);

        UserGamification gamification = new UserGamification();
        gamification.setUserId(user.getId());
        gamificationRepository.save(gamification);

        Lesson lesson = new Lesson();
        lesson.setTitle("Integration lesson");
        lesson.setDifficulty(DifficultyLevel.EASY);
        lesson.setBaseXp(150);
        lesson = lessonRepository.save(lesson);

        Exercise exercise = new Exercise();
        exercise.setLesson(lesson);
        exercise.setQuestion("2 + 2?");
        exercise.setAnswerKey("4");
        exercise.setPoints(5);
        exercise = exerciseRepository.save(exercise);

        ExerciseAttempt attempt = new ExerciseAttempt();
        attempt.setUserId(user.getId());
        attempt.setExerciseId(exercise.getId());
        attempt.setCorrect(true);
        attemptRepository.save(attempt);

        CompletionRequest request = new CompletionRequest();
        request.setReflectionText("I learned how addition combines two values into one correct result today");
        var completion = completionService.complete(user.getId(), lesson.getId(), request);

        assertThat(completion.isPassed()).isTrue();
        assertThat(completion.getXpEarned()).isEqualTo(155);

        UUID userId = user.getId();
        var gamificationAfterFirstCompletion = gamificationRepository.findById(userId).orElseThrow();
        assertThat(gamificationAfterFirstCompletion.getTotalXp()).isEqualTo(155);
        assertThat(gamificationAfterFirstCompletion.getCurrentStreak()).isEqualTo(1);
        var xpTransaction = xpTransactionRepository
                .findByUserIdAndSourceTypeAndSourceId(userId, "LESSON_COMPLETION", lesson.getId())
                .orElseThrow();
        LocalDate completionDate = completion.getCompletedAt().atZoneSameInstant(QuestService.DAY_ZONE).toLocalDate();
        DailyQuest lessonQuest = dailyQuestRepository.findByUserIdAndQuestDateOrderByTypeAsc(userId, completionDate)
                .stream().filter(quest -> quest.getType() == QuestType.COMPLETE_LESSON)
                .findFirst().orElseThrow();
        assertThat(lessonQuest.getProgress()).isEqualTo(1);

        var retry = completionService.complete(userId, lesson.getId(), request);
        assertThat(retry.getCompletionId()).isEqualTo(completion.getCompletionId());
        assertThat(retry.getXpEarned()).isEqualTo(155);
        assertThat(completionRepository.countByUserId(userId)).isEqualTo(1);
        assertThat(xpTransactionRepository
                .findByUserIdAndSourceTypeAndSourceId(userId, "LESSON_COMPLETION", lesson.getId())
                .orElseThrow().getId()).isEqualTo(xpTransaction.getId());
        assertThat(dailyQuestRepository.findByUserIdAndQuestDateOrderByTypeAsc(userId, completionDate)
                .stream().filter(quest -> quest.getType() == QuestType.COMPLETE_LESSON)
                .findFirst().orElseThrow().getProgress()).isEqualTo(1);

        await(Duration.ofSeconds(15), () ->
                gamificationRepository.findById(userId)
                        .map(state -> state.getTotalXp() >= 155 && state.getCurrentStreak() == 1)
                        .orElse(false));
        await(Duration.ofSeconds(15), () ->
                xpTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                        .stream().anyMatch(tx -> "LESSON_COMPLETION".equals(tx.getSourceType())));
        await(Duration.ofSeconds(15), () ->
                !rankHistoryRepository.findByUserIdOrderByChangedAtDesc(userId).isEmpty());
    }

    @Test
    void leaderboardRefreshCanReplaceSameDaySnapshot() {
        leaderboardSnapshotService.refreshSnapshots();
        long firstCount = leaderboardSnapshotRepository.count();

        leaderboardSnapshotService.refreshSnapshots();

        assertThat(firstCount).isGreaterThan(0);
        assertThat(leaderboardSnapshotRepository.count()).isEqualTo(firstCount);
    }

    private void await(Duration timeout, BooleanSupplier condition) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(100);
        }
        assertThat(condition.getAsBoolean()).as("condition met within " + timeout).isTrue();
    }
}
