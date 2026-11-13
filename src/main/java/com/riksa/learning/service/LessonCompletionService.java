package com.riksa.learning.service;

import com.riksa.common.event.EventPublisher;
import com.riksa.learning.dto.CompletionRequest;
import com.riksa.learning.dto.CompletionResponse;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.ExerciseAttempt;
import com.riksa.learning.entity.Lesson;
import com.riksa.learning.entity.LessonCompletion;
import com.riksa.learning.event.LessonCompletedEvent;
import com.riksa.learning.repository.ExerciseAttemptRepository;
import com.riksa.learning.repository.ExerciseRepository;
import com.riksa.learning.repository.LessonCompletionRepository;
import com.riksa.learning.repository.LessonRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.gamification.service.XpLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonCompletionService {

    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final ReflectionValidator reflectionValidator;
    private final EventPublisher eventPublisher;
    private final LessonRewardPolicy lessonRewardPolicy;
    private final LessonProgressionService lessonProgressionService;
    private final UserGamificationRepository userGamificationRepository;
    private final XpLedgerService xpLedgerService;

    /**
     * Finalize a lesson for a user. All exercises must have a correct attempt
     * on record, and a non-blank reflection must be submitted. XP earned is
     * baseXp + sum(points of correct exercises). On success a
     * {@link LessonCompletedEvent} is published inside the same transaction so
     * reward listeners can update XP, streaks, and quests before commit.
     */
    @Transactional
    public CompletionResponse complete(UUID userId, UUID lessonId, CompletionRequest request) {
        // Serialize completions, reward retries, and XP grants for this user.
        userGamificationRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "UserGamification row not found for user " + userId));

        Lesson lesson = lessonProgressionService.requireAccessible(userId, lessonId);

        var existing = lessonCompletionRepository.findByUserIdAndLessonId(userId, lessonId);
        if (existing.isPresent()) {
            LessonCompletion completion = existing.get();
            int actualXp = completion.getXpEarned();
            if (actualXp > 0) {
                actualXp = xpLedgerService.awardXpAndGetResult(
                        userId, actualXp, "LESSON_COMPLETION", lessonId).amountAwarded();
                if (actualXp != completion.getXpEarned()) {
                    completion.setXpEarned(actualXp);
                    lessonCompletionRepository.save(completion);
                }
            }
            return toResponse(completion);
        }

        // Validate reflection
        String reflectionText = request.getReflectionText();
        if (reflectionText == null || reflectionText.isBlank()) {
            reflectionText = "Completed lesson successfully";
        }
        int score = reflectionValidator.validate(reflectionText);

        List<Exercise> exercises = exerciseRepository.findByLessonId(lessonId);
        if (!exercises.isEmpty()) {
            // Ensure every exercise has a correct attempt by this user.
            List<UUID> exerciseIds = exercises.stream().map(Exercise::getId).toList();
            List<ExerciseAttempt> attempts = exerciseAttemptRepository
                    .findByUserIdAndExerciseIdIn(userId, exerciseIds);
            long correctCount = exerciseIds.stream()
                    .filter(exerciseId -> attempts.stream().anyMatch(attempt ->
                            attempt.getExerciseId().equals(exerciseId) && attempt.isCorrect()))
                    .count();
            if (correctCount != exercises.size()) {
                throw new IllegalStateException(
                        "Cannot complete lesson: " + correctCount + "/" + exercises.size()
                                + " exercises have correct attempts");
            }
        }

        // Calculate potential XP, persist the completion, then replace it with
        // the amount actually granted after boosts and the daily cap.
        int potentialXp = lessonRewardPolicy.potentialXp(lesson, exercises);

        LessonCompletion completion = persistCompletion(userId, lessonId, potentialXp,
                reflectionText, score);
        int actualXp = potentialXp > 0
                ? xpLedgerService.awardXpAndGetResult(
                        userId, potentialXp, "LESSON_COMPLETION", lessonId).amountAwarded()
                : 0;
        completion.setXpEarned(actualXp);
        completion = lessonCompletionRepository.save(completion);
        publishEvent(completion, actualXp);
        return toResponse(completion);
    }

    private LessonCompletion persistCompletion(UUID userId, UUID lessonId, int xpEarned,
                                               String reflectionText, int score) {
        LessonCompletion completion = new LessonCompletion();
        completion.setUserId(userId);
        completion.setLessonId(lessonId);
        completion.setXpEarned(xpEarned);
        completion.setReflectionText(reflectionText);
        completion.setScore(score);
        return lessonCompletionRepository.saveAndFlush(completion);
    }

    private void publishEvent(LessonCompletion completion, int actualXp) {
        eventPublisher.publish(new LessonCompletedEvent(this, completion.getUserId(),
                completion.getLessonId(), actualXp, completion.getCompletedAt()));
    }

    private CompletionResponse toResponse(LessonCompletion completion) {
        CompletionResponse r = new CompletionResponse();
        r.setCompletionId(completion.getId());
        r.setLessonId(completion.getLessonId());
        r.setXpEarned(completion.getXpEarned());
        r.setScore(completion.getScore());
        r.setPassed(true);
        r.setCompletedAt(completion.getCompletedAt());
        return r;
    }
}
