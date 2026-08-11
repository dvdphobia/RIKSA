package com.riksa.admin.service;

import com.riksa.admin.dto.ExerciseStatsResponse;
import com.riksa.admin.dto.GamificationStatsResponse;
import com.riksa.admin.dto.OverviewStatsResponse;
import com.riksa.admin.dto.SecurityStatsResponse;
import com.riksa.auth.repository.UserRepository;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.common.enums.DifficultyLevel;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.repository.XpTransactionRepository;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.repository.ExerciseAttemptRepository;
import com.riksa.learning.repository.ExerciseRepository;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.report.enums.ReportStatus;
import com.riksa.report.repository.ContentReportRepository;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private LessonCompletionRepository lessonCompletionRepository;
    @Mock private BlogPostRepository blogPostRepository;
    @Mock private ContentReportRepository reportRepository;
    @Mock private ExerciseAttemptRepository exerciseAttemptRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private XpTransactionRepository xpTransactionRepository;
    @Mock private UserGamificationRepository userGamificationRepository;

    @InjectMocks private AdminStatsService adminStatsService;

    @Test
    void getOverview_mapsAllCounts() {
        when(userRepository.countByCreatedAtAfter(any())).thenReturn(5L, 30L);
        when(postRepository.countByCreatedAtAfter(any())).thenReturn(3L, 20L);
        when(commentRepository.countByCreatedAtAfter(any())).thenReturn(2L, 15L);
        when(lessonCompletionRepository.countByCompletedAtAfter(any())).thenReturn(1L, 10L);
        when(blogPostRepository.countByStatus(BlogStatus.PENDING)).thenReturn(4L);
        when(reportRepository.countByStatus(ReportStatus.OPEN)).thenReturn(7L);

        OverviewStatsResponse response = adminStatsService.getOverview();

        assertThat(response.getSignups24h()).isEqualTo(5);
        assertThat(response.getSignups7d()).isEqualTo(30);
        assertThat(response.getPosts24h()).isEqualTo(3);
        assertThat(response.getPosts7d()).isEqualTo(20);
        assertThat(response.getComments24h()).isEqualTo(2);
        assertThat(response.getComments7d()).isEqualTo(15);
        assertThat(response.getLessonCompletions24h()).isEqualTo(1);
        assertThat(response.getLessonCompletions7d()).isEqualTo(10);
        assertThat(response.getPendingBlogs()).isEqualTo(4);
        assertThat(response.getOpenReports()).isEqualTo(7);
    }

    @Test
    void getExerciseStats_mapsRatesAndSortsWorstFirst() {
        UUID brokenId = UUID.randomUUID();
        UUID goodId = UUID.randomUUID();

        ExerciseAttemptRepository.ExerciseAttemptStats broken = attemptStats(brokenId, 100, 5);
        ExerciseAttemptRepository.ExerciseAttemptStats good = attemptStats(goodId, 100, 90);
        when(exerciseAttemptRepository.findAttemptStats(10)).thenReturn(List.of(good, broken));

        Exercise brokenExercise = exercise(brokenId, "Broken question", DifficultyLevel.HARD);
        Exercise goodExercise = exercise(goodId, "Good question", DifficultyLevel.EASY);
        when(exerciseRepository.findAllById(List.of(goodId, brokenId)))
                .thenReturn(List.of(brokenExercise, goodExercise));

        List<ExerciseStatsResponse> result = adminStatsService.getExerciseStats(10);

        assertThat(result).hasSize(2);

        ExerciseStatsResponse worst = result.get(0);
        assertThat(worst.getExerciseId()).isEqualTo(brokenId);
        assertThat(worst.getQuestion()).isEqualTo("Broken question");
        assertThat(worst.getDifficulty()).isEqualTo("HARD");
        assertThat(worst.getAttempts()).isEqualTo(100);
        assertThat(worst.getCorrectRate()).isEqualTo(5.0);
        assertThat(worst.getFailureRate()).isEqualTo(95.0);

        assertThat(result.get(1).getExerciseId()).isEqualTo(goodId);
        assertThat(result.get(1).getCorrectRate()).isEqualTo(90.0);
    }

    @Test
    void getExerciseStats_emptyStats_returnsEmptyList() {
        when(exerciseAttemptRepository.findAttemptStats(10)).thenReturn(List.of());

        assertThat(adminStatsService.getExerciseStats(10)).isEmpty();
        verifyNoInteractions(exerciseRepository);
    }

    @Test
    void getExerciseStats_missingExercise_nullsDisplayFields() {
        UUID orphanId = UUID.randomUUID();
        ExerciseAttemptRepository.ExerciseAttemptStats orphan = attemptStats(orphanId, 20, 10);
        when(exerciseAttemptRepository.findAttemptStats(10)).thenReturn(List.of(orphan));
        when(exerciseRepository.findAllById(List.of(orphanId))).thenReturn(List.of());

        List<ExerciseStatsResponse> result = adminStatsService.getExerciseStats(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuestion()).isNull();
        assertThat(result.get(0).getDifficulty()).isNull();
        assertThat(result.get(0).getCorrectRate()).isEqualTo(50.0);
    }

    @Test
    void getExerciseStats_zeroMinAttempts_clampsToOne() {
        when(exerciseAttemptRepository.findAttemptStats(1)).thenReturn(List.of());

        adminStatsService.getExerciseStats(0);

        verify(exerciseAttemptRepository).findAttemptStats(1);
    }

    @Test
    void getGamificationStats_mapsRowsAndFillsHistogramBuckets() {
        XpTransactionRepository.DailyXpBySource row = dailyXp("LESSON_COMPLETION",
                LocalDate.of(2026, 8, 5), 250, 12);
        when(xpTransactionRepository.findDailyXpBySourceSince(any())).thenReturn(List.of(row));

        UserGamificationRepository.StreakBucketCount zero = streakBucket("0", 40);
        UserGamificationRepository.StreakBucketCount week = streakBucket("7-13", 9);
        UserGamificationRepository.StreakBucketCount month = streakBucket("30+", 2);
        when(userGamificationRepository.findStreakHistogram()).thenReturn(List.of(zero, week, month));

        GamificationStatsResponse response = adminStatsService.getGamificationStats();

        assertThat(response.getXpByDay()).hasSize(1);
        assertThat(response.getXpByDay().get(0).getDate()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(response.getXpByDay().get(0).getSourceType()).isEqualTo("LESSON_COMPLETION");
        assertThat(response.getXpByDay().get(0).getTotalXp()).isEqualTo(250);
        assertThat(response.getXpByDay().get(0).getTransactionCount()).isEqualTo(12);

        assertThat(response.getStreakHistogram()).extracting(GamificationStatsResponse.StreakBucket::getBucket)
                .containsExactly("0", "1-2", "3-6", "7-13", "14-29", "30+");
        assertThat(response.getStreakHistogram()).extracting(GamificationStatsResponse.StreakBucket::getUserCount)
                .containsExactly(40L, 0L, 0L, 9L, 0L, 2L);
    }

    @Test
    void getGamificationStats_emptyState_returnsEmptyXpAndZeroBuckets() {
        when(xpTransactionRepository.findDailyXpBySourceSince(any())).thenReturn(List.of());
        when(userGamificationRepository.findStreakHistogram()).thenReturn(List.of());

        GamificationStatsResponse response = adminStatsService.getGamificationStats();

        assertThat(response.getXpByDay()).isEmpty();
        assertThat(response.getStreakHistogram()).hasSize(6);
        assertThat(response.getStreakHistogram()).extracting(GamificationStatsResponse.StreakBucket::getUserCount)
                .containsOnly(0L);
    }

    @Test
    void getSecurityStats_mapsCounts() {
        when(userRepository.countByLockedUntilAfter(any())).thenReturn(3L);
        when(userRepository.countBySuspendedUntilAfter(any())).thenReturn(2L);
        when(userRepository.sumFailedLoginAttempts()).thenReturn(127L);

        SecurityStatsResponse response = adminStatsService.getSecurityStats();

        assertThat(response.getLockedAccounts()).isEqualTo(3);
        assertThat(response.getSuspendedAccounts()).isEqualTo(2);
        assertThat(response.getTotalFailedLoginAttempts()).isEqualTo(127);
    }

    private ExerciseAttemptRepository.ExerciseAttemptStats attemptStats(UUID exerciseId, long attempts, long correct) {
        ExerciseAttemptRepository.ExerciseAttemptStats stats = mock(ExerciseAttemptRepository.ExerciseAttemptStats.class);
        when(stats.getExerciseId()).thenReturn(exerciseId);
        when(stats.getAttempts()).thenReturn(attempts);
        when(stats.getCorrectCount()).thenReturn(correct);
        return stats;
    }

    private Exercise exercise(UUID id, String question, DifficultyLevel difficulty) {
        Exercise exercise = new Exercise();
        exercise.setId(id);
        exercise.setQuestion(question);
        exercise.setDifficulty(difficulty);
        return exercise;
    }

    private XpTransactionRepository.DailyXpBySource dailyXp(String sourceType, LocalDate day, long totalXp, long count) {
        XpTransactionRepository.DailyXpBySource row = mock(XpTransactionRepository.DailyXpBySource.class);
        when(row.getSourceType()).thenReturn(sourceType);
        when(row.getDay()).thenReturn(day);
        when(row.getTotalXp()).thenReturn(totalXp);
        when(row.getTransactionCount()).thenReturn(count);
        return row;
    }

    private UserGamificationRepository.StreakBucketCount streakBucket(String bucket, long count) {
        UserGamificationRepository.StreakBucketCount row = mock(UserGamificationRepository.StreakBucketCount.class);
        when(row.getBucket()).thenReturn(bucket);
        when(row.getUserCount()).thenReturn(count);
        return row;
    }
}
