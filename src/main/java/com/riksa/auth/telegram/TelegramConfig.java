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

    @PostConstruct
    public void logConfig() {
        log.info("Telegram config - enabled: {}, botUsername: {}, botToken: {}...{}", 
                enabled, botUsername, 
                botToken != null && botToken.length() > 5 ? botToken.substring(0, 5) : "empty",
                botToken != null && botToken.length() > 5 ? botToken.substring(botToken.length() - 3) : "");
    }
}
