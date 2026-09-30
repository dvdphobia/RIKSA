package com.riksa.auth.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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

    private static final List<String> ALLOWED_UPDATES = List.of("message", "callback_query");

    public void sendMessage(String chatId, String text) {
        sendMessage(chatId, text, null);
    }

    public void sendMessage(String chatId, String text, InlineKeyboard keyboard) {
        sendWithMarkup(chatId, text, keyboard);
    }

    /** Sends with any reply markup: an inline keyboard, a reply keyboard or its removal. */
    public void sendWithMarkup(String chatId, String text, Object markup) {
        if (!telegramConfig.isEnabled()) {
            log.info("Telegram disabled - message delivery skipped");
            return;
        }
        try {
            post("sendMessage", new SendMessageRequest(chatId, text, "HTML", true, markup));
            log.info("Telegram message sent to chatId {}", chatId);
        } catch (Exception e) {
            log.error("Failed to send Telegram message to chatId {} ({})", chatId, e.getClass().getSimpleName());
            throw new RuntimeException("Failed to send Telegram message");
        }
    }

    /** Replaces a bot message and removes its buttons. Best-effort: the state change already happened. */
    public void editMessage(String chatId, Long messageId, String text) {
        if (!telegramConfig.isEnabled() || messageId == null) return;
        try {
            post("editMessageText", new EditMessageRequest(chatId, messageId, text, "HTML", true));
        } catch (Exception e) {
            log.warn("Failed to edit Telegram message in chatId {} ({})", chatId, e.getClass().getSimpleName());
        }
    }

    /** Stops the spinner on a tapped inline button and shows a short toast. */
    public void answerCallback(String callbackQueryId, String text) {
        if (!telegramConfig.isEnabled() || callbackQueryId == null) return;
        try {
            post("answerCallbackQuery", new AnswerCallbackRequest(callbackQueryId, text));
        } catch (Exception e) {
            log.warn("Failed to answer Telegram callback ({})", e.getClass().getSimpleName());
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
        post("setWebhook", new SetWebhookRequest(
                telegramConfig.getWebhookUrl(),
                telegramConfig.getWebhookSecret(),
                ALLOWED_UPDATES
        ));
    }

    /** Shows /code and /help in Telegram's command menu. */
    public void registerCommands() {
        post("setMyCommands", new SetCommandsRequest(List.of(
                new BotCommand("code", "Get a code to sign in to Riksa"),
                new BotCommand("help", "How to sign in with Telegram")
        )));
    }

    /** Required before long polling; Telegram rejects getUpdates while a webhook is set. */
    public void deleteWebhook() {
        post("deleteWebhook", new DeleteWebhookRequest(false));
    }

    public List<TelegramUpdate> getUpdates(long offset, int timeoutSeconds) {
        UpdatesResponse response = RestClient.create().post()
                .uri(url("getUpdates"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new GetUpdatesRequest(offset, timeoutSeconds, ALLOWED_UPDATES))
                .retrieve()
                .body(UpdatesResponse.class);
        return response == null || response.result() == null ? List.of() : response.result();
    }

    private void post(String method, Object body) {
        RestClient.create().post()
                .uri(url(method))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private String url(String method) {
        String base = telegramConfig.getApiBaseUrl() == null || telegramConfig.getApiBaseUrl().isBlank()
                ? "https://api.telegram.org"
                : telegramConfig.getApiBaseUrl().replaceAll("/+$", "");
        return base + "/bot" + telegramConfig.getBotToken() + "/" + method;
    }

    public record InlineKeyboard(List<List<InlineButton>> inline_keyboard) {
        public static InlineKeyboard row(InlineButton... buttons) {
            return new InlineKeyboard(List.of(List.of(buttons)));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InlineButton(String text, String callback_data, String url) {
        public static InlineButton callback(String text, String data) {
            return new InlineButton(text, data, null);
        }

        public static InlineButton link(String text, String url) {
            return new InlineButton(text, null, url);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record SendMessageRequest(
            String chat_id, String text, String parse_mode, boolean disable_web_page_preview,
            Object reply_markup) {}

    /** A keyboard under the chat input, e.g. with a "Share my phone number" button. */
    public record ReplyKeyboard(
            List<List<KeyboardButton>> keyboard, boolean resize_keyboard, boolean one_time_keyboard) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record KeyboardButton(String text, Boolean request_contact) {
        public static KeyboardButton shareContact(String text) {
            return new KeyboardButton(text, true);
        }
    }

    public record ReplyKeyboardRemove(boolean remove_keyboard) {
        public static final ReplyKeyboardRemove INSTANCE = new ReplyKeyboardRemove(true);
    }

    private record EditMessageRequest(
            String chat_id, Long message_id, String text, String parse_mode, boolean disable_web_page_preview) {}

    private record AnswerCallbackRequest(String callback_query_id, String text) {}

    private record SetWebhookRequest(String url, String secret_token, List<String> allowed_updates) {}

    private record DeleteWebhookRequest(boolean drop_pending_updates) {}

    private record GetUpdatesRequest(long offset, int timeout, List<String> allowed_updates) {}

    private record SetCommandsRequest(List<BotCommand> commands) {}

    private record BotCommand(String command, String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UpdatesResponse(boolean ok, List<TelegramUpdate> result) {}
}
