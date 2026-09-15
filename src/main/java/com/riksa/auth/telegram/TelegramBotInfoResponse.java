package com.riksa.auth.telegram;

/**
 * Public bot details for the "Log in with code" screen, which links to the bot before any
 * sign-in request exists. {@code botUrl} opens a plain /start, which replies with a code.
 */
public record TelegramBotInfoResponse(
        boolean available,
        String botUsername,
        String botUrl,
        String botAppUrl,
        int codeLength
) {}
