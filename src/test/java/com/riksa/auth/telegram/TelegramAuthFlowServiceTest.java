package com.riksa.auth.telegram;

import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.otp.OtpService;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.security.JwtService;
import com.riksa.auth.token.RefreshTokenRepository;
import com.riksa.common.config.RiksaRedisProperties;
import com.riksa.gamification.repository.UserGamificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramAuthFlowServiceTest {

    private static final String REQUEST_TOKEN = "A".repeat(43);

    @Mock private TelegramAuthChallengeRepository challengeRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserGamificationRepository userGamificationRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private OtpService otpService;
    @Mock private JwtService jwtService;
    @Mock private TelegramConfig telegramConfig;
    @Mock private TelegramService telegramService;
    @Mock private RedisTelegramChallengeStore redisChallengeStore;
    @Mock private RiksaRedisProperties redisProperties;

    @InjectMocks private TelegramAuthFlowService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshExpiration", 2_592_000_000L);
        lenient().when(redisProperties.isOtpEnabled()).thenReturn(false);
    }

    @Test
    void start_returnsDeepLinksAndStoresOnlyTokenHash() {
        when(telegramConfig.isEnabled()).thenReturn(true);
        when(telegramConfig.getBotToken()).thenReturn("bot-token");
        when(telegramConfig.getBotUsername()).thenReturn("@riksa_bot");
        when(telegramConfig.getWebhookSecret()).thenReturn("webhook-secret");
        when(telegramConfig.getAuthRequestExpirationMinutes()).thenReturn(10);

        TelegramAuthStartResponse response = service.start();

        assertThat(response.requestToken()).hasSize(43);
        assertThat(response.botUrl()).isEqualTo(
                "https://t.me/riksa_bot?start=auth_" + response.requestToken()
        );
        assertThat(response.botAppUrl()).startsWith("tg://resolve?domain=riksa_bot&start=auth_");
        verify(challengeRepository).save(any(TelegramAuthChallenge.class));
    }

    @Test
    void start_whenRedisEnabled_storesChallengeInRedis() {
        when(redisProperties.isOtpEnabled()).thenReturn(true);
        when(telegramConfig.isEnabled()).thenReturn(true);
        when(telegramConfig.getBotToken()).thenReturn("bot-token");
        when(telegramConfig.getBotUsername()).thenReturn("riksa_bot");
        when(telegramConfig.getWebhookSecret()).thenReturn("webhook-secret");
        when(telegramConfig.getAuthRequestExpirationMinutes()).thenReturn(10);

        service.start();

        verify(redisChallengeStore).save(any(TelegramAuthChallenge.class));
        verify(challengeRepository, never()).save(any());
    }

    @Test
    void webhook_privateStart_capturesTelegramIdentityAndSendsCode() {
        TelegramAuthChallenge challenge = pendingChallenge();
        when(telegramConfig.getWebhookSecret()).thenReturn("webhook-secret");
        when(challengeRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(challenge));

        TelegramUpdate update = new TelegramUpdate(
                1L,
                new TelegramUpdate.TelegramMessage(
                        new TelegramUpdate.TelegramChat(777L, "private"),
                        new TelegramUpdate.TelegramUser(777L, false, "Sok", "Sophea", "sophea"),
                        "/start auth_" + REQUEST_TOKEN
                )
        );

        service.handleWebhook("webhook-secret", update);

        assertThat(challenge.getTelegramUserId()).isEqualTo("777");
        assertThat(challenge.getTelegramChatId()).isEqualTo("777");
        assertThat(challenge.getTelegramDisplayName()).isEqualTo("Sok Sophea");
        assertThat(challenge.getStatus()).isEqualTo(TelegramAuthChallengeStatus.CODE_SENT);
        verify(otpService).generateOtp("777");
    }

    @Test
    void webhook_wrongSecret_rejectsUpdateBeforeDatabaseLookup() {
        when(telegramConfig.getWebhookSecret()).thenReturn("webhook-secret");

        assertThatThrownBy(() -> service.handleWebhook("wrong", null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Invalid Telegram webhook secret");

        verify(challengeRepository, never()).findByTokenHashForUpdate(anyString());
    }

    @Test
    void completeLogin_existingTelegramUser_updatesChatAndReturnsSession() {
        TelegramAuthChallenge challenge = codeSentChallenge();
        User user = existingUser();
        when(challengeRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(challenge));
        when(otpService.verifyOtp("777", "123456")).thenReturn(true);
        when(userRepository.findByTelegramId("777")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(jwtService.generateAccessToken(user.getId(), "sophea", "USER")).thenReturn("access");
        when(jwtService.generateRefreshToken(user.getId(), "sophea", "USER")).thenReturn("refresh");

        var response = service.completeLogin(new TelegramAuthCompleteRequest(REQUEST_TOKEN, "123456"));

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        assertThat(user.getTelegramChatId()).isEqualTo("777");
        assertThat(challenge.getStatus()).isEqualTo(TelegramAuthChallengeStatus.COMPLETED);
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void completeLogin_newTelegramUser_createsAccountAndGamification() {
        TelegramAuthChallenge challenge = codeSentChallenge();
        challenge.setTelegramUsername("newlearner");
        challenge.setTelegramDisplayName("New Learner");
        when(challengeRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(challenge));
        when(otpService.verifyOtp("777", "123456")).thenReturn(true);
        when(userRepository.findByTelegramId("777")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("newlearner")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(userGamificationRepository.existsByUserId(any(UUID.class))).thenReturn(false);
        when(jwtService.generateAccessToken(any(UUID.class), anyString(), anyString())).thenReturn("access");
        when(jwtService.generateRefreshToken(any(UUID.class), anyString(), anyString())).thenReturn("refresh");

        var response = service.completeLogin(new TelegramAuthCompleteRequest(REQUEST_TOKEN, "123456"));

        assertThat(response.getUsername()).isEqualTo("newlearner");
        verify(userGamificationRepository).save(any());
    }

    private TelegramAuthChallenge pendingChallenge() {
        TelegramAuthChallenge challenge = new TelegramAuthChallenge();
        challenge.setTokenHash("hash");
        challenge.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        return challenge;
    }

    private TelegramAuthChallenge codeSentChallenge() {
        TelegramAuthChallenge challenge = pendingChallenge();
        challenge.setStatus(TelegramAuthChallengeStatus.CODE_SENT);
        challenge.setTelegramUserId("777");
        challenge.setTelegramChatId("777");
        return challenge;
    }

    private User existingUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setTelegramId("777");
        user.setUsername("sophea");
        user.setDisplayName("Sok Sophea");
        user.setRole(Role.USER);
        return user;
    }
}
