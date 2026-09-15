package com.riksa.auth.telegram;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@Setter
@Component
public class TelegramConfig {

    private static final Logger log = LoggerFactory.getLogger(TelegramConfig.class);

    @Value("${telegram.enabled:false}")
    private boolean enabled;

    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.bot-username:}")
    private String botUsername;

    /** Bot API host. Only changed to point at a local fake server in end-to-end tests. */
    @Value("${telegram.api-base-url:https://api.telegram.org}")
    private String apiBaseUrl;

    @Value("${telegram.webhook-url:}")
    private String webhookUrl;

    @Value("${telegram.webhook-secret:}")
    private String webhookSecret;

    /** Long-poll getUpdates instead of a webhook. For local development without public HTTPS. */
    @Value("${telegram.polling-enabled:false}")
    private boolean pollingEnabled;

    @Value("${telegram.auth-request-expiration-minutes:10}")
    private int authRequestExpirationMinutes;

    @Value("${telegram.login-code-expiration-minutes:5}")
    private int loginCodeExpirationMinutes;

    /** Optional https link for an "Open Riksa" button under bot messages. */
    @Value("${telegram.app-url:}")
    private String appUrl;

    @PostConstruct
    public void logConfig() {
        log.info("Telegram config - enabled: {}, botUsername configured: {}, botToken configured: {}, webhook configured: {}, polling: {}",
                enabled,
                botUsername != null && !botUsername.isBlank(),
                botToken != null && !botToken.isBlank(),
                webhookUrl != null && !webhookUrl.isBlank()
                        && webhookSecret != null && !webhookSecret.isBlank(),
                pollingEnabled);
    }

    /** Whether the bot can receive /start and button taps at all. */
    public boolean receivesUpdates() {
        return pollingEnabled || (webhookSecret != null && !webhookSecret.isBlank());
    }
}
