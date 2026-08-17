package com.riksa.auth.telegram;

import java.time.OffsetDateTime;

public record TelegramAuthStartResponse(
        String requestToken,
        String botUrl,
        String botAppUrl,
        OffsetDateTime expiresAt,
        int pollAfterSeconds
) {}

