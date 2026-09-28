package com.riksa.moderation;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps small per-author mutation windows in memory for this single-instance
 * application. Expired keys are pruned on each mutation check, and rejected
 * requests do not add another timestamp.
 */
@Component
public class AuthorSlidingWindowRateLimiter {

    private final Map<RateLimitKey, WindowState> actions = new HashMap<>();

    public synchronized void checkAndRecord(String action, UUID authorId, int maximum, Duration window) {
        Instant now = Instant.now();
        pruneExpiredActions(now);
        RateLimitKey key = new RateLimitKey(action, authorId);
        WindowState state = actions.computeIfAbsent(key, ignored -> new WindowState(window));
        if (state.timestamps.size() >= maximum) {
            throw new RateLimitedException();
        }
        state.timestamps.addLast(now);
    }

    private void pruneExpiredActions(Instant now) {
        actions.entrySet().removeIf(entry -> {
            WindowState state = entry.getValue();
            Instant cutoff = now.minus(state.window);
            while (!state.timestamps.isEmpty() && !state.timestamps.peekFirst().isAfter(cutoff)) {
                state.timestamps.removeFirst();
            }
            return state.timestamps.isEmpty();
        });
    }

    private record RateLimitKey(String action, UUID authorId) {
    }

    private static final class WindowState {
        private final Duration window;
        private final ArrayDeque<Instant> timestamps = new ArrayDeque<>();

        private WindowState(Duration window) {
            this.window = window;
        }
    }
}
