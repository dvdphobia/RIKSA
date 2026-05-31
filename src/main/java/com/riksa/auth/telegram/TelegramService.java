package com.riksa.auth.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramService {

    private final TelegramConfig telegramConfig;

    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";

    public void sendMessage(String chatId, String text) {
        if (!telegramConfig.isEnabled()) {
            log.info("Telegram disabled - would have sent message to chatId {}: {}", chatId, text);
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
            log.error("Failed to send Telegram message to chatId {}: {}", chatId, e.getMessage());
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

    private record SendMessageRequest(String chat_id, String text, String parse_mode) {}
}
