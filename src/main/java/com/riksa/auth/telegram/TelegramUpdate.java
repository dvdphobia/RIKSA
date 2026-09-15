package com.riksa.auth.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramUpdate(
        @JsonProperty("update_id") Long updateId,
        TelegramMessage message,
        @JsonProperty("callback_query") TelegramCallbackQuery callbackQuery
) {
    public TelegramUpdate(Long updateId, TelegramMessage message) {
        this(updateId, message, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramMessage(
            @JsonProperty("message_id") Long messageId,
            TelegramChat chat,
            TelegramUser from,
            String text
    ) {
        public TelegramMessage(TelegramChat chat, TelegramUser from, String text) {
            this(null, chat, from, text);
        }
    }

    /** Sent when the user taps an inline button such as Confirm. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramCallbackQuery(
            String id,
            TelegramUser from,
            TelegramMessage message,
            String data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramChat(Long id, String type) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramUser(
            Long id,
            @JsonProperty("is_bot") Boolean bot,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            String username
    ) {}
}
