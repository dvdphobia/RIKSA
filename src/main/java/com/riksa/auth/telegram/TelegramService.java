package com.riksa.auth.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramService {

    private final TelegramConfig telegramConfig;

    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";

    public void sendMessage(String chatId, String text) {
        if (!telegramConfig.isEnabled()) {
            log.info("Telegram disabled - message delivery skipped");
            return;
        }

        String url = TELEGRAM_API_BASE + telegramConfig.getBotToken() + "/sendMessage";

        try {
            RestClient restClient = RestClient.create();
            restClient.post()
                    .uri(url)
                    .body(new SendMessageRequest(chatId, text, "HTML"))
                    .retrieve()
                    .toEntity(String.class);
            log.info("Telegram message sent to chatId {}", chatId);
        } catch (Exception e) {
            log.error("Failed to send Telegram message to chatId {} ({})", chatId, e.getClass().getSimpleName());
            throw new RuntimeException("Failed to send Telegram message");
        }
    }

    public void sendOtp(String chatId, String code) {
        String text = String.format(
                "<b>🔐 Riksa Verification Code</b>\n\n" +
                "Your verification code is: <code>%s</code>\n\n" +
                "This code expires in 5 minutes.\n" +
                "If you did not request this code, please ignore this message.",
                code
        );
        sendMessage(chatId, text);
    }

    public void registerWebhook() {
        if (!telegramConfig.isEnabled()
                || telegramConfig.getWebhookUrl() == null
                || telegramConfig.getWebhookUrl().isBlank()) {
            return;
        }
        if (telegramConfig.getWebhookSecret() == null || telegramConfig.getWebhookSecret().isBlank()) {
            throw new IllegalStateException("TELEGRAM_WEBHOOK_SECRET is required when TELEGRAM_WEBHOOK_URL is set");
        }

        String url = TELEGRAM_API_BASE + telegramConfig.getBotToken() + "/setWebhook";
        RestClient.create().post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SetWebhookRequest(
                        telegramConfig.getWebhookUrl(),
                        telegramConfig.getWebhookSecret(),
                        List.of("message")
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private record SendMessageRequest(String chat_id, String text, String parse_mode) {}

    private record SetWebhookRequest(String url, String secret_token, List<String> allowed_updates) {}
}
