package com.riksa.auth.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramUpdate(
        @JsonProperty("update_id") Long updateId,
        TelegramMessage message
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramMessage(
            TelegramChat chat,
            TelegramUser from,
            String text
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
