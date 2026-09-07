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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExerciseAttemptService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final LessonProgressionService lessonProgressionService;

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

        String submitted = request.getAnswer();
        boolean correct = isCorrect(exercise, submitted);

        // Append-only history: once correct, an exercise remains passed.
        ExerciseAttempt attempt = new ExerciseAttempt();
        attempt.setUserId(userId);
        attempt.setExerciseId(exerciseId);
        attempt.setSubmittedAnswer(submitted);
        attempt.setAttemptNumber(Math.toIntExact(
                exerciseAttemptRepository.countByUserIdAndExerciseId(userId, exerciseId) + 1));
        attempt.setCorrect(correct);
        attempt.setAttemptedAt(java.time.OffsetDateTime.now());
        exerciseAttemptRepository.save(attempt);

        String message = correct ? "Correct!" : "Not quite. Try again.";
        return new AnswerResult(exerciseId, correct, exercise.getPoints(), message,
                correct ? null : exercise.getHint(), exercise.getExplanation(), attempt.getAttemptNumber());
    }

    private boolean isCorrect(Exercise exercise, String submitted) {
        if (submitted == null || exercise.getAnswerKey() == null) {
            return false;
        }
        String normalizedSubmitted = normalize(submitted);
        String normalizedKey = normalize(exercise.getAnswerKey());

        if (normalizedKey.equals(normalizedSubmitted)) {
            return true;
        }

        List<String> options = exercise.getOptions();
        if (options != null && !options.isEmpty()) {
            try {
                int index = Integer.parseInt(submitted.trim());
                if (index >= 0 && index < options.size()) {
                    String selectedOption = options.get(index);
                    if (normalize(selectedOption).equals(normalizedKey)) {
                        return true;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return false;
    }

    private String normalize(String s) {
        return s.trim().toLowerCase();
    }
}
