package com.riksa.learning.service;

import com.riksa.auth.security.RateLimitDecision;
import com.riksa.auth.security.RedisRateLimiter;
import com.riksa.common.exception.RateLimitExceededException;
import com.riksa.common.exception.ServiceUnavailableException;
import com.riksa.learning.dto.AnswerResult;
import com.riksa.learning.dto.ExerciseAttemptRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuestExerciseCheckService {
    private static final String RATE_LIMIT_BUCKET = "guest-answer-check";
    private static final long RATE_LIMIT = 30;
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);

    private final RedisRateLimiter rateLimiter;
    private final ExerciseAttemptService exerciseAttemptService;

    public AnswerResult check(
            String clientIp,
            UUID lessonId,
            UUID exerciseId,
            ExerciseAttemptRequest request) {
        RateLimitDecision decision;
        try {
            decision = rateLimiter.acquire(RATE_LIMIT_BUCKET,
                    clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim(),
                    RATE_LIMIT, RATE_WINDOW);
        } catch (RuntimeException e) {
            throw new ServiceUnavailableException("Answer checking is temporarily unavailable", e);
        }
        if (!decision.allowed()) {
            throw new RateLimitExceededException(
                    "Too many answer checks. Try again later.", decision.retryAfterSeconds());
        }
        return exerciseAttemptService.checkAnswer(lessonId, exerciseId, request);
    }
}
