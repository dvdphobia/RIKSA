package com.riksa.learning.service;

import com.riksa.common.event.EventPublisher;
import com.riksa.common.exception.ResourceNotFoundException;
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

    /**
     * Finalize a lesson for a user. All exercises must have a correct attempt
     * on record, and a non-blank reflection must be submitted. XP earned is
     * baseXp + sum(points of correct exercises). On success a
     * {@link LessonCompletedEvent} is published so the gamification listeners
     * can award XP, update streaks, and check achievements.
     *
     * @throws IllegalStateException if the lesson was already completed by the user
     */
    @Transactional
    public CompletionResponse complete(UUID userId, UUID lessonId, CompletionRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));

        if (lessonCompletionRepository.existsByUserIdAndLessonId(userId, lessonId)) {
            throw new IllegalStateException("Lesson already completed: " + lessonId);
        }

        List<Exercise> exercises = exerciseRepository.findByLessonId(lessonId);
        if (exercises.isEmpty()) {
            // A lesson with no exercises is trivially "all correct"
            int xp = lesson.getBaseXp();
            int score = reflectionValidator.validate(request.getReflectionText());
            LessonCompletion completion = persistCompletion(userId, lessonId, xp, request.getReflectionText(), score);
            publishEvent(userId, lessonId, xp);
            return toResponse(completion);
        }

        // Ensure every exercise has a correct attempt by this user
        List<UUID> exerciseIds = exercises.stream().map(Exercise::getId).toList();
        List<ExerciseAttempt> attempts = exerciseAttemptRepository.findByUserIdAndExerciseIdIn(userId, exerciseIds);
        long correctCount = attempts.stream().filter(ExerciseAttempt::isCorrect).count();
        if (correctCount != exercises.size()) {
            throw new IllegalStateException(
                    "Cannot complete lesson: " + correctCount + "/" + exercises.size()
                            + " exercises have correct attempts");
        }

        // Validate reflection
        int score = reflectionValidator.validate(request.getReflectionText());

        // XP = baseXp + sum of correct exercise points (all are correct at this point)
        int exerciseXp = exercises.stream().mapToInt(Exercise::getPoints).sum();
        int xpEarned = lesson.getBaseXp() + exerciseXp;

        LessonCompletion completion = persistCompletion(userId, lessonId, xpEarned,
                request.getReflectionText(), score);
        publishEvent(userId, lessonId, xpEarned);
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
        return lessonCompletionRepository.save(completion);
    }

    private void publishEvent(UUID userId, UUID lessonId, int xpEarned) {
        eventPublisher.publish(new LessonCompletedEvent(this, userId, lessonId, xpEarned));
    }

    private CompletionResponse toResponse(LessonCompletion completion) {
        CompletionResponse r = new CompletionResponse();
        r.setLessonId(completion.getLessonId());
        r.setXpEarned(completion.getXpEarned());
        r.setScore(completion.getScore());
        r.setCompletedAt(completion.getCompletedAt());
        return r;
    }
}