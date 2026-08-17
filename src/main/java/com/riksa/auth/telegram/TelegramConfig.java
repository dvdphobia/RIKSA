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

    @Value("${telegram.webhook-url:}")
    private String webhookUrl;

    @Value("${telegram.webhook-secret:}")
    private String webhookSecret;

    @Value("${telegram.auth-request-expiration-minutes:10}")
    private int authRequestExpirationMinutes;

    @PostConstruct
    public void logConfig() {
        log.info("Telegram config - enabled: {}, botUsername configured: {}, botToken configured: {}, webhook configured: {}",
                enabled,
                botUsername != null && !botUsername.isBlank(),
                botToken != null && !botToken.isBlank(),
                webhookUrl != null && !webhookUrl.isBlank()
                        && webhookSecret != null && !webhookSecret.isBlank());
    }
}
