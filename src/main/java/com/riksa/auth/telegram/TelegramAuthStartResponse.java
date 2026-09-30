package com.riksa.auth.telegram;

import java.time.OffsetDateTime;

/**
 * {@code requestToken} stays in the app and is the only way to poll. The deep links carry a
 * separate one-time nonce, so a leaked link cannot be used to collect the session.
 */
public record TelegramAuthStartResponse(
        String requestToken,
        String botUrl,
        String botAppUrl,
        String botUsername,
        OffsetDateTime expiresAt,
        long expiresInSeconds,
        int pollIntervalSeconds
) {}
