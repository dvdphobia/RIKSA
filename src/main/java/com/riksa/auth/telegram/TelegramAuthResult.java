package com.riksa.auth.telegram;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.riksa.auth.dto.AuthResponse;

import java.time.OffsetDateTime;

/**
 * Outcome of polling a one-tap request or redeeming a bot code.
 *
 * <ul>
 *   <li>{@code PENDING}: keep polling ({@code expiresAt} set)</li>
 *   <li>{@code AUTHENTICATED}: existing account, {@code auth} set</li>
 *   <li>{@code PROFILE_REQUIRED}: new Telegram user, {@code signupToken} and {@code telegram} set</li>
 *   <li>{@code DENIED} / {@code EXPIRED}: start again</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelegramAuthResult(
        Status status,
        AuthResponse auth,
        String signupToken,
        TelegramProfile telegram,
        OffsetDateTime expiresAt
) {
    public enum Status { PENDING, AUTHENTICATED, PROFILE_REQUIRED, DENIED, EXPIRED }

    public record TelegramProfile(String displayName, String username) {}

    static TelegramAuthResult of(Status status) {
        return new TelegramAuthResult(status, null, null, null, null);
    }

    static TelegramAuthResult pending(OffsetDateTime expiresAt) {
        return new TelegramAuthResult(Status.PENDING, null, null, null, expiresAt);
    }

    static TelegramAuthResult authenticated(AuthResponse auth) {
        return new TelegramAuthResult(Status.AUTHENTICATED, auth, null, null, null);
    }

    static TelegramAuthResult profileRequired(String signupToken, TelegramAuthChallenge challenge) {
        return new TelegramAuthResult(
                Status.PROFILE_REQUIRED,
                null,
                signupToken,
                new TelegramProfile(challenge.getTelegramDisplayName(), challenge.getTelegramUsername()),
                challenge.getExpiresAt()
        );
    }
}
