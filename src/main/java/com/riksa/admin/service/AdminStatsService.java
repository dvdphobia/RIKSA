package com.riksa.admin.service;

import com.riksa.admin.dto.ExerciseStatsResponse;
import com.riksa.admin.dto.GamificationStatsResponse;
import com.riksa.admin.dto.OverviewStatsResponse;
import com.riksa.admin.dto.SecurityStatsResponse;
import com.riksa.auth.repository.UserRepository;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.repository.BlogPostRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final List<String> STREAK_BUCKETS = List.of("0", "1-2", "3-6", "7-13", "14-29", "30+");

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final BlogPostRepository blogPostRepository;
    private final ContentReportRepository reportRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ExerciseRepository exerciseRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final UserGamificationRepository userGamificationRepository;

    @Transactional(readOnly = true)
    public OverviewStatsResponse getOverview() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime last24h = now.minus(Duration.ofHours(24));
        OffsetDateTime last7d = now.minus(Duration.ofDays(7));

        return new OverviewStatsResponse(
                userRepository.countByCreatedAtAfter(last24h),
                userRepository.countByCreatedAtAfter(last7d),
                postRepository.countByCreatedAtAfter(last24h),
                postRepository.countByCreatedAtAfter(last7d),
                commentRepository.countByCreatedAtAfter(last24h),
                commentRepository.countByCreatedAtAfter(last7d),
                lessonCompletionRepository.countByCompletedAtAfter(last24h),
                lessonCompletionRepository.countByCompletedAtAfter(last7d),
                blogPostRepository.countByStatus(BlogStatus.PENDING),
                reportRepository.countByStatus(ReportStatus.OPEN)
        );
    }

    @Transactional(readOnly = true)
    public List<ExerciseStatsResponse> getExerciseStats(int minAttempts) {
        int safeMinAttempts = Math.max(1, minAttempts);

        List<ExerciseAttemptRepository.ExerciseAttemptStats> stats =
                exerciseAttemptRepository.findAttemptStats(safeMinAttempts);
        if (stats.isEmpty()) {
            return List.of();
        }

        List<UUID> exerciseIds = stats.stream()
                .map(ExerciseAttemptRepository.ExerciseAttemptStats::getExerciseId)
                .toList();
        Map<UUID, Exercise> exercisesById = exerciseRepository.findAllById(exerciseIds).stream()
                .collect(Collectors.toMap(Exercise::getId, Function.identity()));

        return stats.stream()
                .map(s -> {
                    Exercise exercise = exercisesById.get(s.getExerciseId());
                    double correctRate = s.getAttempts() == 0
                            ? 0.0
                            : round2(s.getCorrectCount() * 100.0 / s.getAttempts());
                    return new ExerciseStatsResponse(
                            s.getExerciseId(),
                            exercise != null ? exercise.getQuestion() : null,
                            exercise != null && exercise.getDifficulty() != null
                                    ? exercise.getDifficulty().name() : null,
                            s.getAttempts(),
                            s.getCorrectCount(),
                            correctRate,
                            round2(100.0 - correctRate)
                    );
                })
                .sorted(Comparator.comparingDouble(ExerciseStatsResponse::getCorrectRate))
                .toList();
    }

    @Transactional(readOnly = true)
    public GamificationStatsResponse getGamificationStats() {
        OffsetDateTime since = OffsetDateTime.now().minus(Duration.ofDays(30));

        List<GamificationStatsResponse.DailyXp> xpByDay = xpTransactionRepository
                .findDailyXpBySourceSince(since).stream()
                .map(row -> new GamificationStatsResponse.DailyXp(
                        row.getDay(), row.getSourceType(), row.getTotalXp(), row.getTransactionCount()))
                .toList();

        Map<String, Long> countsByBucket = userGamificationRepository.findStreakHistogram().stream()
                .collect(Collectors.toMap(
                        UserGamificationRepository.StreakBucketCount::getBucket,
                        UserGamificationRepository.StreakBucketCount::getUserCount));

        List<GamificationStatsResponse.StreakBucket> histogram = STREAK_BUCKETS.stream()
                .map(bucket -> new GamificationStatsResponse.StreakBucket(
                        bucket, countsByBucket.getOrDefault(bucket, 0L)))
                .toList();

        return new GamificationStatsResponse(xpByDay, histogram);
    }

    @Transactional(readOnly = true)
    public SecurityStatsResponse getSecurityStats() {
        OffsetDateTime now = OffsetDateTime.now();
        return new SecurityStatsResponse(
                userRepository.countByLockedUntilAfter(now),
                userRepository.countBySuspendedUntilAfter(now),
                userRepository.sumFailedLoginAttempts()
        );
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
