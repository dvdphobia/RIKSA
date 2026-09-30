package com.riksa.auth.controller;

import com.riksa.auth.telegram.TelegramConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramReturnControllerTest {

    @Test
    void pageOpensTheAppOnAndroidAndIos() {
        String page = new TelegramReturnController().backToApp().getBody();

        assertThat(page).contains(TelegramReturnController.APP_LINK);
        assertThat(page).contains(TelegramReturnController.ANDROID_LINK);
        assertThat(page).contains("package=com.riksa.riksa_app");
    }

    @Test
    void returnUrlDefaultsToThePageNextToTheWebhook() {
        TelegramConfig config = new TelegramConfig();
        ReflectionTestUtils.setField(config, "webhookUrl",
                "https://api.example.com/api/v1/auth/telegram/webhook");

        assertThat(config.getReturnUrl()).isEqualTo("https://api.example.com/api/v1/auth/telegram/return");
    }

    @Test
    void explicitReturnUrlWins() {
        TelegramConfig config = new TelegramConfig();
        ReflectionTestUtils.setField(config, "webhookUrl", "https://api.example.com/api/v1/auth/telegram/webhook");
        ReflectionTestUtils.setField(config, "returnUrl", "https://riksa.app/back");

        assertThat(config.getReturnUrl()).isEqualTo("https://riksa.app/back");
    }

    @Test
    void noWebhookMeansNoButton() {
        assertThat(new TelegramConfig().getReturnUrl()).isNull();
    }
}
