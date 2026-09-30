package com.riksa.auth.telegram;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Links Telegram to a signed-in account with either a confirmed one-tap request or a bot code. */
public record TelegramLinkRequest(
        @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Telegram request token is invalid")
        String requestToken,

        @Size(max = 32, message = "Sign-in code is invalid")
        String code
) {}
