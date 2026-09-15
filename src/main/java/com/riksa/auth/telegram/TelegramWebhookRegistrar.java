package com.riksa.auth.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramWebhookRegistrar {

    private final TelegramConfig telegramConfig;
    private final TelegramService telegramService;

    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        if (!telegramConfig.isEnabled()
                || telegramConfig.getBotToken() == null
                || telegramConfig.getBotToken().isBlank()) {
            return;
        }

        try {
            telegramService.registerCommands();
        } catch (RuntimeException ex) {
            log.warn("Telegram bot command registration failed ({})", ex.getClass().getSimpleName());
        }

        if (telegramConfig.getWebhookUrl() == null || telegramConfig.getWebhookUrl().isBlank()) {
            log.info("Telegram webhook auto-registration skipped; TELEGRAM_WEBHOOK_URL is not configured");
            return;
        }

        try {
            telegramService.registerWebhook();
            log.info("Telegram webhook registered successfully");
        } catch (RuntimeException ex) {
            // The API remains usable for password/Google auth if Telegram is temporarily unavailable.
            log.error("Telegram webhook registration failed ({})", ex.getClass().getSimpleName());
        }
    }
}
