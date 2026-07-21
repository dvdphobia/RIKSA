package com.riksa.learning.service;

import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.learning.dto.AnswerResult;
import com.riksa.learning.dto.ExerciseAttemptRequest;
import com.riksa.learning.entity.Exercise;
import com.riksa.learning.entity.ExerciseAttempt;
import com.riksa.learning.repository.ExerciseAttemptRepository;
import com.riksa.learning.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExerciseAttemptService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;

    /**
     * Submit an answer for a single exercise. The attempt is recorded (upserted)
     * keyed on (userId, exerciseId). Only the latest result is kept.
     *
     * @param userId     the authenticated user
     * @param lessonId   the lesson the exercise belongs to
     * @param exerciseId the exercise being attempted
     * @param request    the request body containing the answer
     * @return the answer result (correct/incorrect, points, message)
     * @throws ResourceNotFoundException if the exercise does not exist or does not
     *                                   belong to the given lesson
     */
    @Transactional
    public AnswerResult submitAttempt(UUID userId, UUID lessonId, UUID exerciseId, ExerciseAttemptRequest request) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found: " + exerciseId));

        if (exercise.getLesson() == null || !exercise.getLesson().getId().equals(lessonId)) {
            throw new ResourceNotFoundException("Exercise " + exerciseId + " does not belong to lesson " + lessonId);
        }

        String answerKey = exercise.getAnswerKey();
        String submitted = request.getAnswer();
        boolean correct = answerKey != null
                && submitted != null
                && normalize(answerKey).equals(normalize(submitted));

        // Upsert: keep only the latest attempt per (user, exercise)
        ExerciseAttempt attempt = exerciseAttemptRepository
                .findByUserIdAndExerciseId(userId, exerciseId)
                .orElseGet(ExerciseAttempt::new);

        if (attempt.getId() == null) {
            attempt.setUserId(userId);
            attempt.setExerciseId(exerciseId);
        }
        attempt.setCorrect(correct);
        attempt.setAttemptedAt(java.time.OffsetDateTime.now());
        exerciseAttemptRepository.save(attempt);

        String message = correct
                ? "Correct! +" + exercise.getPoints() + " XP"
                : "Incorrect. The correct answer did not match your submission.";
        return new AnswerResult(exerciseId, correct, exercise.getPoints(), message);
    }

    private String normalize(String s) {
        return s.trim().toLowerCase();
    }
}