package com.riksa.auth.telegram;

import org.springframework.web.util.HtmlUtils;

/** Bot copy (Telegram HTML parse mode). Everything user-supplied goes through {@link #escape}. */
final class TelegramBotMessages {

    private static final String DIVIDER = "────────────────────";

    private TelegramBotMessages() {}

    static String confirmPrompt(String displayName, String username, boolean accountExists, long minutesLeft) {
        String who = "<b>" + escape(fallbackName(displayName)) + "</b>"
                + (username == null || username.isBlank() ? "" : " (@" + escape(username) + ")");
        return "🔐 <b>Sign in to Riksa?</b>\n\n"
                + "Telegram account: " + who + "\n"
                + (accountExists
                        ? "👋 Welcome back! You'll go straight to your account.\n"
                        : "✨ No Riksa account yet. You'll choose a username in the app next.\n")
                + "\n⚠️ Only confirm if <b>you</b> just tapped <i>Open Telegram</i> in the Riksa app. "
                + "Never confirm a link someone sent you.\n\n"
                + "⏱ This request expires in " + minutesLeft + " min.";
    }

    static String confirmed() {
        return "✅ <b>Confirmed!</b>\n\nGo back to the Riksa app. You'll be signed in automatically.";
    }

    static String denied() {
        return "✖️ <b>Sign-in cancelled.</b>\n\nNobody was signed in. If you didn't start this, you can ignore it.";
    }

    static String alreadyConfirmed() {
        return "✅ Already confirmed. Return to the Riksa app to continue.";
    }

    static String linkExpired() {
        return "⌛ <b>This sign-in link has expired or was already used.</b>\n\n"
                + "Go back to Riksa and tap <b>Open Telegram</b> again, or send /code to sign in with a code.";
    }

    static String linkOpenedByAnotherAccount() {
        return "🚫 This sign-in link was already opened by a different Telegram account.\n\n"
                + "Go back to Riksa and start again.";
    }

    static String loginCode(String firstName, String code, int minutes, boolean accountExists) {
        String name = escape(fallbackName(firstName));
        return (accountExists
                        ? "👋 <b>Welcome back, " + name + "!</b>\n\n"
                        : "👋 <b>Welcome to Riksa, " + name + "!</b>\n\n")
                + "🔑 <b>Your sign-in code</b>\n"
                + "<pre>" + code + "</pre>\n"
                + "<i>Tap the code to copy it.</i>\n\n"
                + "⏱ Expires in " + minutes + " minutes · works once\n"
                + "🛡 Never share this code. Riksa will never ask you for it.\n"
                + DIVIDER + "\n"
                + "📲 In Riksa, tap <b>Continue with Telegram → Log in with code</b> and paste it.\n"
                + (accountExists ? "" : "✨ No account yet? You'll pick a username right after.\n")
                + "🔁 Need a new code? Send /code";
    }

    static String tooManyCodes(long retryAfterSeconds) {
        long minutes = Math.max(1, (retryAfterSeconds + 59) / 60);
        return "⏳ You've requested several codes already. Try again in " + minutes + " min, "
                + "or use the most recent code you received.";
    }

    static String unavailable() {
        return "⚠️ Sign-in is temporarily unavailable. Please try again in a moment.";
    }

    static String help() {
        return "🎓 <b>Riksa sign-in</b>\n\n"
                + "<b>Two ways to sign in:</b>\n"
                + "1️⃣ In the Riksa app tap <b>Continue with Telegram → Open Telegram</b>, then tap Confirm here.\n"
                + "2️⃣ Send /code, copy the code, and paste it in <b>Log in with code</b>.\n\n"
                + "New to Riksa? Both options create your account.";
    }

    private static String fallbackName(String name) {
        return name == null || name.isBlank() ? "there" : name;
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value);
    }
}
