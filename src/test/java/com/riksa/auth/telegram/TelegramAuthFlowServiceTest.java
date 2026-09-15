package com.riksa.auth.telegram;

import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.otp.OtpHasher;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.security.JwtService;
import com.riksa.auth.security.RateLimitDecision;
import com.riksa.auth.security.RedisRateLimiter;
import com.riksa.auth.token.RefreshTokenRepository;
import com.riksa.common.config.RiksaRedisProperties;
import com.riksa.common.exception.GoneException;
import com.riksa.gamification.repository.UserGamificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramAuthFlowServiceTest {

    private static final long TELEGRAM_ID = 777L;
    private static final Pattern LINK = Pattern.compile("https://t\\.me/riksa_bot\\?start=login_([a-f0-9]{32})");
    private static final Pattern CODE = Pattern.compile("<pre>([A-Z0-9]{8})</pre>");

    @Mock private TelegramAuthChallengeRepository challengeRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserGamificationRepository userGamificationRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private TelegramConfig telegramConfig;
    @Mock private TelegramService telegramService;
    @Mock private RedisTelegramChallengeStore redisChallengeStore;
    @Mock private RiksaRedisProperties redisProperties;
    @Mock private RedisRateLimiter rateLimiter;

    private TelegramAuthFlowService service;

    /** In-memory stand-in for the challenge table so state transitions are exercised for real. */
    private final List<TelegramAuthChallenge> challenges = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new TelegramAuthFlowService(
                challengeRepository, userRepository, userGamificationRepository, refreshTokenRepository,
                jwtService, telegramConfig, telegramService, redisChallengeStore, redisProperties,
                new OtpHasher("a-secure-test-secret-that-is-at-least-32-characters"), rateLimiter);
        ReflectionTestUtils.setField(service, "refreshExpiration", 2_592_000_000L);

        lenient().when(redisProperties.isOtpEnabled()).thenReturn(false);
        lenient().when(telegramConfig.isEnabled()).thenReturn(true);
        lenient().when(telegramConfig.getBotToken()).thenReturn("bot-token");
        lenient().when(telegramConfig.getBotUsername()).thenReturn("@riksa_bot");
        lenient().when(telegramConfig.getWebhookSecret()).thenReturn("webhook-secret");
        lenient().when(telegramConfig.receivesUpdates()).thenReturn(true);
        lenient().when(telegramConfig.getAuthRequestExpirationMinutes()).thenReturn(10);
        lenient().when(telegramConfig.getLoginCodeExpirationMinutes()).thenReturn(5);
        lenient().when(rateLimiter.acquire(anyString(), anyString(), anyLong(), any(Duration.class)))
                .thenReturn(new RateLimitDecision(true, 5, 4, 0));

        lenient().when(challengeRepository.save(any(TelegramAuthChallenge.class))).thenAnswer(invocation -> {
            TelegramAuthChallenge challenge = invocation.getArgument(0);
            if (!challenges.contains(challenge)) challenges.add(challenge);
            return challenge;
        });
        lenient().when(challengeRepository.findByTokenHashForUpdate(anyString())).thenAnswer(invocation ->
                challenges.stream().filter(c -> c.getTokenHash().equals(invocation.getArgument(0))).findFirst());
        lenient().when(challengeRepository.findByStartNonceHashForUpdate(anyString())).thenAnswer(invocation ->
                challenges.stream().filter(c -> invocation.getArgument(0).equals(c.getStartNonceHash())).findFirst());

        lenient().when(userRepository.findByTelegramId(anyString())).thenReturn(Optional.empty());
        lenient().when(jwtService.generateAccessToken(any(UUID.class), anyString(), anyString())).thenReturn("access");
        lenient().when(jwtService.generateRefreshToken(any(UUID.class), anyString(), anyString())).thenReturn("refresh");
    }

    // ------------------------------------------------------------ one-tap link

    @Test
    void start_returnsLoginLinkWhoseNonceIsNotTheRequestToken() {
        TelegramAuthStartResponse response = service.start();

        Matcher link = LINK.matcher(response.botUrl());
        assertThat(link.matches()).isTrue();
        assertThat(response.requestToken()).hasSize(43).doesNotContain(link.group(1));
        assertThat(response.botAppUrl()).isEqualTo("tg://resolve?domain=riksa_bot&start=login_" + link.group(1));
        assertThat(response.pollIntervalSeconds()).isEqualTo(3);
        assertThat(challenges).singleElement().satisfies(challenge -> {
            assertThat(challenge.getStatus()).isEqualTo(TelegramAuthChallengeStatus.PENDING);
            assertThat(challenge.getStartNonceHash()).isNotBlank().isNotEqualTo(link.group(1));
        });
    }

    @Test
    void botInfo_linksToPlainStartThatRepliesWithCode() {
        TelegramBotInfoResponse info = service.botInfo();

        assertThat(info.available()).isTrue();
        assertThat(info.botUsername()).isEqualTo("riksa_bot");
        assertThat(info.botUrl()).isEqualTo("https://t.me/riksa_bot?start=code");

        service.handleUpdate(message(TELEGRAM_ID, "/start code"));
        verify(telegramService).sendMessage(eq("777"), contains("<pre>"), isNull());
    }

    @Test
    void start_withoutWebhookOrPolling_isNotConfigured() {
        when(telegramConfig.receivesUpdates()).thenReturn(false);

        assertThatThrownBy(() -> service.start())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Telegram login is not configured");
    }

    @Test
    void openingLink_bindsTelegramUserAndAsksForConfirmation() {
        String nonce = nonce(service.start());

        service.handleUpdate(message(TELEGRAM_ID, "/start login_" + nonce));

        TelegramAuthChallenge challenge = challenges.get(0);
        assertThat(challenge.getStatus()).isEqualTo(TelegramAuthChallengeStatus.PENDING);
        assertThat(challenge.getTelegramUserId()).isEqualTo("777");
        assertThat(challenge.getTelegramDisplayName()).isEqualTo("Sok Sophea");
        ArgumentCaptor<TelegramService.InlineKeyboard> keyboard = ArgumentCaptor.forClass(TelegramService.InlineKeyboard.class);
        verify(telegramService).sendMessage(eq("777"), contains("Sign in to Riksa?"), keyboard.capture());
        assertThat(keyboard.getValue().inline_keyboard().get(0))
                .extracting(TelegramService.InlineButton::callback_data)
                .containsExactly("tga:ok:" + nonce, "tga:no:" + nonce);
    }

    @Test
    void openingLink_fromSecondTelegramAccount_isRejected() {
        String nonce = nonce(service.start());
        service.handleUpdate(message(TELEGRAM_ID, "/start login_" + nonce));

        service.handleUpdate(message(999L, "/start login_" + nonce));

        assertThat(challenges.get(0).getTelegramUserId()).isEqualTo("777");
        verify(telegramService).sendMessage(eq("999"), contains("different Telegram account"));
    }

    @Test
    void confirmButton_onlyWorksForTheTelegramUserWhoOpenedTheLink() {
        TelegramAuthStartResponse start = service.start();
        String nonce = nonce(start);
        service.handleUpdate(message(TELEGRAM_ID, "/start login_" + nonce));

        service.handleUpdate(callback(999L, "tga:ok:" + nonce));
        assertThat(service.status(start.requestToken()).status()).isEqualTo(TelegramAuthResult.Status.PENDING);

        service.handleUpdate(callback(TELEGRAM_ID, "tga:ok:" + nonce));
        verify(telegramService).editMessage(eq("777"), eq(42L), contains("Confirmed"));
        assertThat(challenges.get(0).getStatus()).isEqualTo(TelegramAuthChallengeStatus.CONFIRMED);
    }

    @Test
    void notMeButton_deniesTheRequest() {
        TelegramAuthStartResponse start = service.start();
        String nonce = nonce(start);
        service.handleUpdate(message(TELEGRAM_ID, "/start login_" + nonce));

        service.handleUpdate(callback(TELEGRAM_ID, "tga:no:" + nonce));

        assertThat(service.status(start.requestToken()).status()).isEqualTo(TelegramAuthResult.Status.DENIED);
    }

    @Test
    void status_afterConfirm_signsExistingAccountInOnce() {
        TelegramAuthStartResponse start = confirmedLink();
        User user = existingUser();
        when(userRepository.findByTelegramId("777")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        TelegramAuthResult result = service.status(start.requestToken());

        assertThat(result.status()).isEqualTo(TelegramAuthResult.Status.AUTHENTICATED);
        assertThat(result.auth().getAccessToken()).isEqualTo("access");
        assertThat(user.getTelegramChatId()).isEqualTo("777");
        assertThat(service.status(start.requestToken()).status()).isEqualTo(TelegramAuthResult.Status.EXPIRED);
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void status_afterConfirm_newTelegramUser_needsProfileWithPrefill() {
        TelegramAuthStartResponse start = confirmedLink();

        TelegramAuthResult result = service.status(start.requestToken());

        assertThat(result.status()).isEqualTo(TelegramAuthResult.Status.PROFILE_REQUIRED);
        assertThat(result.signupToken()).isEqualTo(start.requestToken());
        assertThat(result.telegram().displayName()).isEqualTo("Sok Sophea");
        assertThat(result.telegram().username()).isEqualTo("sophea");
        assertThat(result.auth()).isNull();
        // Polling again is harmless while the user types their profile.
        assertThat(service.status(start.requestToken()).status()).isEqualTo(TelegramAuthResult.Status.PROFILE_REQUIRED);
    }

    // ------------------------------------------------------------ bot code

    @Test
    void plainStart_repliesWithSingleUseCode_thatSignsExistingUserIn() {
        String code = requestCode(TELEGRAM_ID);
        User user = existingUser();
        when(userRepository.findByTelegramId("777")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        String typed = code.substring(0, 4).toLowerCase() + "-" + code.substring(4);
        TelegramAuthResult result = service.redeemCode(typed);

        assertThat(result.status()).isEqualTo(TelegramAuthResult.Status.AUTHENTICATED);
        assertThatThrownBy(() -> service.redeemCode(code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid or has expired");
    }

    @Test
    void codeCommand_forNewTelegramUser_returnsFreshSignupToken() {
        String code = requestCode(TELEGRAM_ID);

        TelegramAuthResult result = service.redeemCode(code);

        assertThat(result.status()).isEqualTo(TelegramAuthResult.Status.PROFILE_REQUIRED);
        assertThat(result.signupToken()).hasSize(43);
        assertThat(challenges).extracting(TelegramAuthChallenge::getStatus)
                .containsExactly(TelegramAuthChallengeStatus.COMPLETED, TelegramAuthChallengeStatus.PROFILE_REQUIRED);
    }

    @Test
    void codeMessage_explainsUsageAndNeverLeaksIntoStorage() {
        String code = requestCode(TELEGRAM_ID);

        verify(telegramService).sendMessage(eq("777"), contains("Log in with code"), isNull());
        assertThat(challenges.get(0).getStartNonceHash()).doesNotContain(code);
        assertThat(challenges.get(0).getStatus()).isEqualTo(TelegramAuthChallengeStatus.CONFIRMED);
    }

    @Test
    void tooManyCodeRequests_areRefusedInTheBot() {
        when(rateLimiter.acquire(anyString(), anyString(), anyLong(), any(Duration.class)))
                .thenReturn(new RateLimitDecision(false, 5, 0, 240));

        service.handleUpdate(message(TELEGRAM_ID, "/code"));

        assertThat(challenges).isEmpty();
        verify(telegramService).sendMessage(eq("777"), contains("Try again in 4 min"));
    }

    @Test
    void malformedOrUnknownCodes_areRejected() {
        assertThatThrownBy(() -> service.redeemCode("123"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.redeemCode("ABCDEFGH"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------ profile step

    @Test
    void completeSignup_createsAccountWithChosenNames() {
        String signupToken = service.redeemCode(requestCode(TELEGRAM_ID)).signupToken();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var auth = service.completeSignup(new TelegramAuthCompleteRequest(signupToken, " sophea ", "Sok Sophea"));

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(user.capture());
        assertThat(user.getValue().getUsername()).isEqualTo("sophea");
        assertThat(user.getValue().getTelegramId()).isEqualTo("777");
        assertThat(auth.getUsername()).isEqualTo("sophea");
        verify(userGamificationRepository).save(any());
        assertThatThrownBy(() -> service.completeSignup(new TelegramAuthCompleteRequest(signupToken, "other", "X")))
                .isInstanceOf(GoneException.class);
    }

    @Test
    void completeSignup_takenUsername_keepsTheSignupTokenUsable() {
        String signupToken = service.redeemCode(requestCode(TELEGRAM_ID)).signupToken();
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.completeSignup(new TelegramAuthCompleteRequest(signupToken, "taken", "Sok")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Username is already taken");

        assertThat(challenges.get(1).getStatus()).isEqualTo(TelegramAuthChallengeStatus.PROFILE_REQUIRED);
        verify(userRepository, never()).save(any());
    }

    @Test
    void completeSignup_unknownToken_isGone() {
        assertThatThrownBy(() -> service.completeSignup(
                new TelegramAuthCompleteRequest("B".repeat(43), "sophea", "Sok")))
                .isInstanceOf(GoneException.class);
    }

    @Test
    void webhook_wrongSecret_rejectsUpdateBeforeLookup() {
        assertThatThrownBy(() -> service.handleWebhook("wrong", message(TELEGRAM_ID, "/code")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Invalid Telegram webhook secret");

        assertThat(challenges).isEmpty();
    }

    // ------------------------------------------------------------ helpers

    private TelegramAuthStartResponse confirmedLink() {
        TelegramAuthStartResponse start = service.start();
        String nonce = nonce(start);
        service.handleUpdate(message(TELEGRAM_ID, "/start login_" + nonce));
        service.handleUpdate(callback(TELEGRAM_ID, "tga:ok:" + nonce));
        return start;
    }

    private String requestCode(long telegramId) {
        service.handleUpdate(message(telegramId, "/start"));
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(telegramService).sendMessage(eq(Long.toString(telegramId)), text.capture(), isNull());
        Matcher code = CODE.matcher(text.getValue());
        assertThat(code.find()).isTrue();
        return code.group(1);
    }

    private static String nonce(TelegramAuthStartResponse response) {
        Matcher link = LINK.matcher(response.botUrl());
        assertThat(link.matches()).isTrue();
        return link.group(1);
    }

    private static TelegramUpdate message(long telegramId, String text) {
        return new TelegramUpdate(1L, new TelegramUpdate.TelegramMessage(
                new TelegramUpdate.TelegramChat(telegramId, "private"),
                new TelegramUpdate.TelegramUser(telegramId, false, "Sok", "Sophea", "sophea"),
                text));
    }

    private static TelegramUpdate callback(long telegramId, String data) {
        return new TelegramUpdate(2L, null, new TelegramUpdate.TelegramCallbackQuery(
                "cb-1",
                new TelegramUpdate.TelegramUser(telegramId, false, "Sok", "Sophea", "sophea"),
                new TelegramUpdate.TelegramMessage(42L, new TelegramUpdate.TelegramChat(telegramId, "private"), null, null),
                data));
    }

    private static User existingUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setTelegramId("777");
        user.setUsername("sophea");
        user.setDisplayName("Sok Sophea");
        user.setRole(Role.USER);
        return user;
    }
}
