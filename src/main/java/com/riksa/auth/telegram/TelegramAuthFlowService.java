package com.riksa.auth.telegram;

import com.riksa.auth.avatar.AvatarCatalog;
import com.riksa.auth.dto.AuthResponse;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.otp.OtpService;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.security.JwtService;
import com.riksa.auth.token.RefreshToken;
import com.riksa.auth.token.RefreshTokenRepository;
import com.riksa.common.config.RiksaRedisProperties;
import com.riksa.common.exception.ServiceUnavailableException;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.repository.UserGamificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TelegramAuthFlowService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern REQUEST_TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final String START_PAYLOAD_PREFIX = "auth_";

    private final TelegramAuthChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final UserGamificationRepository userGamificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final TelegramConfig telegramConfig;
    private final TelegramService telegramService;
    private final RedisTelegramChallengeStore redisChallengeStore;
    private final RiksaRedisProperties redisProperties;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public TelegramAuthStartResponse start() {
        requireAvailable();

        String requestToken = newRequestToken();
        OffsetDateTime expiresAt = OffsetDateTime.now()
                .plusMinutes(telegramConfig.getAuthRequestExpirationMinutes());

        TelegramAuthChallenge challenge = new TelegramAuthChallenge();
        challenge.setTokenHash(hash(requestToken));
        challenge.setExpiresAt(expiresAt);
        saveChallenge(challenge);

        String botUsername = normalizedBotUsername();
        String payload = START_PAYLOAD_PREFIX + requestToken;
        return new TelegramAuthStartResponse(
                requestToken,
                "https://t.me/" + botUsername + "?start=" + payload,
                "tg://resolve?domain=" + botUsername + "&start=" + payload,
                expiresAt,
                3
        );
    }

    @Transactional
    public TelegramAuthStatusResponse status(String requestToken) {
        TelegramAuthChallenge challenge = findChallenge(requestToken, false);
        expireIfNeeded(challenge);

        boolean codeSent = challenge.getStatus() == TelegramAuthChallengeStatus.CODE_SENT;
        Boolean accountExists = codeSent
                ? userRepository.existsByTelegramId(challenge.getTelegramUserId())
                : null;

        return new TelegramAuthStatusResponse(
                challenge.getStatus(),
                codeSent,
                accountExists,
                challenge.getExpiresAt()
        );
    }

    @Transactional
    public void handleWebhook(String providedSecret, TelegramUpdate update) {
        validateWebhookSecret(providedSecret);

        TelegramUpdate.TelegramMessage message = update == null ? null : update.message();
        if (message == null || message.chat() == null || message.from() == null || message.text() == null) {
            return;
        }
        if (!"private".equals(message.chat().type())
                || Boolean.TRUE.equals(message.from().bot())
                || !message.chat().id().equals(message.from().id())) {
            return;
        }

        String[] command = message.text().trim().split("\\s+", 2);
        String botCommand = command.length == 0 ? "" : command[0].toLowerCase(Locale.ROOT);
        if (!botCommand.equals("/start") && !botCommand.startsWith("/start@")) {
            return;
        }

        String chatId = message.chat().id().toString();
        if (command.length != 2 || !command[1].startsWith(START_PAYLOAD_PREFIX)) {
            telegramService.sendMessage(chatId, "Open Riksa and tap <b>Continue with Telegram</b> to request a login code.");
            return;
        }

        String requestToken = command[1].substring(START_PAYLOAD_PREFIX.length());
        if (!REQUEST_TOKEN_PATTERN.matcher(requestToken).matches()) {
            telegramService.sendMessage(chatId, "This Riksa login link is invalid. Please request a new one in the app.");
            return;
        }

        Optional<TelegramAuthChallenge> found = findChallengeOptional(hash(requestToken), true);
        if (found.isEmpty()) {
            telegramService.sendMessage(chatId, "This Riksa login link is invalid or expired. Please request a new one in the app.");
            return;
        }

        TelegramAuthChallenge challenge = found.get();
        expireIfNeeded(challenge);
        if (challenge.getStatus() == TelegramAuthChallengeStatus.EXPIRED
                || challenge.getStatus() == TelegramAuthChallengeStatus.COMPLETED) {
            telegramService.sendMessage(chatId, "This Riksa login link has expired or was already used. Please request a new one in the app.");
            return;
        }

        String telegramUserId = message.from().id().toString();
        if (challenge.getStatus() == TelegramAuthChallengeStatus.CODE_SENT) {
            if (telegramUserId.equals(challenge.getTelegramUserId())) {
                telegramService.sendMessage(chatId, "A Riksa verification code was already sent for this request. Return to the app and enter it there.");
            }
            return;
        }

        challenge.setTelegramUserId(telegramUserId);
        challenge.setTelegramChatId(chatId);
        challenge.setTelegramUsername(trimTo(message.from().username(), 64));
        challenge.setTelegramDisplayName(telegramDisplayName(message.from()));

        otpService.generateOtp(chatId);
        challenge.setStatus(TelegramAuthChallengeStatus.CODE_SENT);
        saveChallenge(challenge);
    }

    @Transactional
    public AuthResponse completeLogin(TelegramAuthCompleteRequest request) {
        TelegramAuthChallenge challenge = verifyCode(request);

        User user = userRepository.findByTelegramId(challenge.getTelegramUserId()).orElse(null);
        if (user == null) {
            user = createTelegramUser(challenge);
        } else {
            requireActive(user);
            user.setTelegramChatId(challenge.getTelegramChatId());
            user.setPhoneVerified(true);
            user = userRepository.save(user);
        }

        complete(challenge);
        return buildAuthResponse(user);
    }

    @Transactional
    public void link(UUID userId, TelegramAuthCompleteRequest request) {
        TelegramAuthChallenge challenge = verifyCode(request);
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        requireActive(target);

        userRepository.findByTelegramId(challenge.getTelegramUserId())
                .filter(existing -> !existing.getId().equals(target.getId()))
                .ifPresent(existing -> {
                    throw new IllegalStateException("This Telegram account is already linked to another Riksa account");
                });
        userRepository.findByTelegramChatId(challenge.getTelegramChatId())
                .filter(existing -> !existing.getId().equals(target.getId()))
                .ifPresent(existing -> {
                    throw new IllegalStateException("This Telegram chat is already linked to another Riksa account");
                });

        target.setTelegramId(challenge.getTelegramUserId());
        target.setTelegramChatId(challenge.getTelegramChatId());
        target.setPhoneVerified(true);
        userRepository.save(target);
        complete(challenge);
    }

    private TelegramAuthChallenge verifyCode(TelegramAuthCompleteRequest request) {
        TelegramAuthChallenge challenge = findChallenge(request.requestToken(), true);
        expireIfNeeded(challenge);
        if (challenge.getStatus() == TelegramAuthChallengeStatus.EXPIRED) {
            throw new IllegalArgumentException("Telegram login request has expired");
        }
        if (challenge.getStatus() == TelegramAuthChallengeStatus.COMPLETED) {
            throw new IllegalArgumentException("Telegram login request was already used");
        }
        if (challenge.getStatus() != TelegramAuthChallengeStatus.CODE_SENT
                || challenge.getTelegramChatId() == null
                || challenge.getTelegramUserId() == null) {
            throw new IllegalStateException("Open the Telegram bot and tap Start before entering a code");
        }
        if (!otpService.verifyOtp(challenge.getTelegramChatId(), request.code())) {
            throw new IllegalArgumentException("Invalid or expired OTP code");
        }
        return challenge;
    }

    private User createTelegramUser(TelegramAuthChallenge challenge) {
        User user = new User();
        user.setTelegramId(challenge.getTelegramUserId());
        user.setTelegramChatId(challenge.getTelegramChatId());
        user.setUsername(uniqueUsername(challenge));
        user.setDisplayName(defaultDisplayName(challenge));
        user.setRole(Role.USER);
        user.setPhoneVerified(true);
        user.setAvatarUrl(randomAvatar());
        user = userRepository.save(user);

        if (!userGamificationRepository.existsByUserId(user.getId())) {
            UserGamification gamification = new UserGamification();
            gamification.setUserId(user.getId());
            userGamificationRepository.save(gamification);
        }
        return user;
    }

    private String uniqueUsername(TelegramAuthChallenge challenge) {
        String base = challenge.getTelegramUsername() == null
                ? ""
                : challenge.getTelegramUsername().toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9_]", "");
        if (base.length() < 3) {
            base = "tg_" + challenge.getTelegramUserId();
        }
        base = trimTo(base, 50);

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            String suffixText = "_" + suffix++;
            candidate = trimTo(base, 50 - suffixText.length()) + suffixText;
        }
        return candidate;
    }

    private String defaultDisplayName(TelegramAuthChallenge challenge) {
        String displayName = challenge.getTelegramDisplayName();
        return displayName == null || displayName.isBlank() ? "Telegram User" : displayName;
    }

    private String telegramDisplayName(TelegramUpdate.TelegramUser telegramUser) {
        String firstName = telegramUser.firstName() == null ? "" : telegramUser.firstName().trim();
        String lastName = telegramUser.lastName() == null ? "" : telegramUser.lastName().trim();
        return trimTo((firstName + " " + lastName).trim(), 100);
    }

    private void complete(TelegramAuthChallenge challenge) {
        challenge.setStatus(TelegramAuthChallengeStatus.COMPLETED);
        challenge.setCompletedAt(OffsetDateTime.now());
        saveChallenge(challenge);
    }

    private TelegramAuthChallenge findChallenge(String requestToken, boolean lock) {
        if (requestToken == null || !REQUEST_TOKEN_PATTERN.matcher(requestToken).matches()) {
            throw new IllegalArgumentException("Telegram request token is invalid");
        }
        Optional<TelegramAuthChallenge> challenge = findChallengeOptional(hash(requestToken), lock);
        return challenge.orElseThrow(() -> new IllegalArgumentException("Telegram login request was not found"));
    }

    private void expireIfNeeded(TelegramAuthChallenge challenge) {
        if (challenge.getStatus() != TelegramAuthChallengeStatus.COMPLETED
                && challenge.getStatus() != TelegramAuthChallengeStatus.EXPIRED
                && challenge.getExpiresAt().isBefore(OffsetDateTime.now())) {
            challenge.setStatus(TelegramAuthChallengeStatus.EXPIRED);
            saveChallenge(challenge);
        }
    }

    private Optional<TelegramAuthChallenge> findChallengeOptional(String tokenHash, boolean lock) {
        if (redisProperties.isOtpEnabled()) {
            Optional<TelegramAuthChallenge> redisChallenge;
            try {
                redisChallenge = redisChallengeStore.find(tokenHash);
            } catch (RuntimeException e) {
                throw new ServiceUnavailableException("Telegram authentication is temporarily unavailable", e);
            }
            if (redisChallenge.isPresent()) return redisChallenge;
        }
        return lock
                ? challengeRepository.findByTokenHashForUpdate(tokenHash)
                : challengeRepository.findByTokenHash(tokenHash);
    }

    private void saveChallenge(TelegramAuthChallenge challenge) {
        if (redisProperties.isOtpEnabled() && challenge.getId() == null) {
            try {
                redisChallengeStore.save(challenge);
            } catch (RuntimeException e) {
                throw new ServiceUnavailableException("Telegram authentication is temporarily unavailable", e);
            }
        } else {
            challengeRepository.save(challenge);
        }
    }

    private void validateWebhookSecret(String providedSecret) {
        String configuredSecret = telegramConfig.getWebhookSecret();
        if (configuredSecret == null || configuredSecret.isBlank() || providedSecret == null
                || !MessageDigest.isEqual(
                        configuredSecret.getBytes(StandardCharsets.UTF_8),
                        providedSecret.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("Invalid Telegram webhook secret");
        }
    }

    private void requireAvailable() {
        if (!telegramConfig.isEnabled()) {
            throw new IllegalStateException("Telegram login is currently disabled");
        }
        if (telegramConfig.getBotToken() == null || telegramConfig.getBotToken().isBlank()
                || telegramConfig.getBotUsername() == null || telegramConfig.getBotUsername().isBlank()
                || telegramConfig.getWebhookSecret() == null || telegramConfig.getWebhookSecret().isBlank()) {
            throw new IllegalStateException("Telegram login is not configured");
        }
    }

    private String normalizedBotUsername() {
        String username = telegramConfig.getBotUsername().trim();
        return username.startsWith("@") ? username.substring(1) : username;
    }

    private void requireActive(User user) {
        if (user.isSuspended()) {
            throw new AccessDeniedException("Account suspended until " + user.getSuspendedUntil());
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String refreshTokenJwt = jwtService.generateRefreshToken(user.getId(), user.getUsername(), user.getRole().name());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenJwt);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(OffsetDateTime.now().plus(Duration.ofMillis(refreshExpiration)));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                user.getId(),
                accessToken,
                refreshTokenJwt,
                user.getUsername(),
                user.getRole().name()
        );
    }

    private String randomAvatar() {
        var avatars = AvatarCatalog.availableAvatars();
        return avatars.get(RANDOM.nextInt(avatars.size()));
    }

    private String newRequestToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String trimTo(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
