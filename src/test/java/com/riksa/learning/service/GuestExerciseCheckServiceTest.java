package com.riksa.learning.service;

import com.riksa.auth.security.RateLimitDecision;
import com.riksa.auth.security.RedisRateLimiter;
import com.riksa.common.exception.RateLimitExceededException;
import com.riksa.learning.dto.AnswerResult;
import com.riksa.learning.dto.ExerciseAttemptRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestExerciseCheckServiceTest {
    @Mock RedisRateLimiter rateLimiter;
    @Mock ExerciseAttemptService exerciseAttemptService;

    @Test
    void checksAnswerForAllowedClientIp() {
        UUID lessonId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        ExerciseAttemptRequest request = new ExerciseAttemptRequest();
        request.setAnswer("42");
        AnswerResult expected = new AnswerResult(exerciseId, true, 5, "Correct!", null, "42 is the answer.", 0);
        when(rateLimiter.acquire("guest-answer-check", "192.0.2.40", 30, Duration.ofMinutes(1)))
                .thenReturn(new RateLimitDecision(true, 30, 29, 60));
        when(exerciseAttemptService.checkAnswer(lessonId, exerciseId, request)).thenReturn(expected);

        AnswerResult result = new GuestExerciseCheckService(rateLimiter, exerciseAttemptService)
                .check("192.0.2.40", lessonId, exerciseId, request);

        assertThat(result).isSameAs(expected);
        verify(exerciseAttemptService).checkAnswer(lessonId, exerciseId, request);
    }

    @Test
    void rateLimitReturns429ExceptionWithoutCheckingOrSavingAnAnswer() {
        UUID lessonId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        ExerciseAttemptRequest request = new ExerciseAttemptRequest();
        request.setAnswer("42");
        when(rateLimiter.acquire(eq("guest-answer-check"), eq("192.0.2.40"), eq(30L), eq(Duration.ofMinutes(1))))
                .thenReturn(new RateLimitDecision(false, 30, 0, 17));

        assertThatThrownBy(() -> new GuestExerciseCheckService(rateLimiter, exerciseAttemptService)
                .check("192.0.2.40", lessonId, exerciseId, request))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("Too many answer checks. Try again later.")
                .satisfies(error -> assertThat(((RateLimitExceededException) error).getRetryAfterSeconds())
                        .isEqualTo(17));

        verifyNoInteractions(exerciseAttemptService);
    }
}
