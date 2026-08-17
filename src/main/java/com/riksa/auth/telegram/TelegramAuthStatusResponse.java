package com.riksa.auth.telegram;

import java.time.OffsetDateTime;

public record TelegramAuthStatusResponse(
        TelegramAuthChallengeStatus status,
        boolean codeSent,
        Boolean accountExists,
        OffsetDateTime expiresAt
) {}

