package com.riksa.auth.telegram;

public enum TelegramAuthChallengeStatus {
    /** Link created; waiting for the user to open the bot and tap Confirm. */
    PENDING,
    /** Telegram identity confirmed (Confirm button, or a freshly issued bot code). */
    CONFIRMED,
    /** The user tapped "Not me" in the bot. */
    DENIED,
    /** Confirmed Telegram user without a Riksa account; waiting for username and display name. */
    PROFILE_REQUIRED,
    COMPLETED,
    EXPIRED
}
