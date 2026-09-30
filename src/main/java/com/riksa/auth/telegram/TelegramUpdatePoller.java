package com.riksa.auth.telegram;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Local development only: Telegram cannot reach {@code localhost}, so with
 * {@code TELEGRAM_POLLING_ENABLED=true} (and no webhook URL) the API pulls updates itself.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramUpdatePoller {

    private static final int LONG_POLL_SECONDS = 25;

    private final TelegramConfig telegramConfig;
    private final TelegramService telegramService;
    private final TelegramAuthFlowService flowService;

    private volatile boolean running;
    private Thread worker;

    @EventListener(ApplicationReadyEvent.class)
    public void startIfEnabled() {
        if (!telegramConfig.isEnabled() || !telegramConfig.isPollingEnabled()) return;
        if (telegramConfig.getWebhookUrl() != null && !telegramConfig.getWebhookUrl().isBlank()) {
            log.warn("TELEGRAM_POLLING_ENABLED ignored because TELEGRAM_WEBHOOK_URL is set");
            return;
        }
        if (telegramConfig.getBotToken() == null || telegramConfig.getBotToken().isBlank()) {
            log.warn("Telegram polling skipped; TELEGRAM_BOT_TOKEN is not configured");
            return;
        }

        running = true;
        worker = new Thread(this::run, "telegram-update-poller");
        worker.setDaemon(true);
        worker.start();
        log.info("Telegram long polling started (development mode)");
    }

    private void run() {
        long offset = 0;
        boolean webhookCleared = false;
        while (running) {
            try {
                if (!webhookCleared) {
                    telegramService.deleteWebhook();
                    webhookCleared = true;
                }
                List<TelegramUpdate> updates = telegramService.getUpdates(offset, LONG_POLL_SECONDS);
                for (TelegramUpdate update : updates) {
                    if (update.updateId() != null) offset = Math.max(offset, update.updateId() + 1);
                    try {
                        flowService.handleUpdate(update);
                    } catch (RuntimeException ex) {
                        log.warn("Telegram update {} failed ({})", update.updateId(), ex.getClass().getSimpleName());
                    }
                }
            } catch (RuntimeException ex) {
                if (!running) return;
                log.warn("Telegram polling failed ({}); retrying in 5s", ex.getClass().getSimpleName());
                sleep(5_000);
            }
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (worker != null) worker.interrupt();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
