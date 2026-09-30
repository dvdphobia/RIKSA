package com.riksa.auth.telegram;

import com.riksa.auth.avatar.AvatarCatalog;
import com.riksa.auth.dto.AuthResponse;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.otp.OtpHasher;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.security.JwtService;
import com.riksa.auth.security.RateLimitDecision;
import com.riksa.auth.security.RedisRateLimiter;
import com.riksa.auth.token.RefreshToken;
import com.riksa.auth.token.RefreshTokenRepository;
import com.riksa.common.config.RiksaRedisProperties;
import com.riksa.common.exception.GoneException;
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
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Telegram sign-in and sign-up. Two entry points share one account rule (stable Telegram user id):
 *
 * <ol>
 *   <li><b>One-tap</b>: {@link #start()} returns {@code t.me/<bot>?start=login_<nonce>}. The bot asks
 *       the user to Confirm, and {@link #status(String)} then signs in or asks for a profile.</li>
 *   <li><b>Code</b>: {@code /start} or {@code /code} in the bot issues a short code that
 *       {@link #redeemCode(String)} exchanges.</li>
 * </ol>
 *
 * New Telegram users finish with {@link #completeSignup(TelegramAuthCompleteRequest)}.
 */
@Service
@RequiredArgsConstructor
public class TelegramAuthFlowService {

    static final String LOGIN_PAYLOAD_PREFIX = "login_";
    static final String CALLBACK_CONFIRM = "tga:ok:";
    static final String CALLBACK_DENY = "tga:no:";
    static final int CODE_LENGTH = 8;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern REQUEST_TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final Pattern LINK_NONCE_PATTERN = Pattern.compile("[a-f0-9]{32}");
    /** No 0/O, 1/I: codes are read on one screen and typed or pasted on another. */
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Duration PROFILE_WINDOW = Duration.ofMinutes(15);
    private static final Duration CLAIM_TTL = Duration.ofMinutes(30);
    private static final int CODES_PER_WINDOW = 5;
    private static final Duration CODE_WINDOW = Duration.ofMinutes(10);
    private static final int POLL_INTERVAL_SECONDS = 3;

    private final TelegramAuthChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final UserGamificationRepository userGamificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final TelegramConfig telegramConfig;
    private final TelegramService telegramService;
    private final RedisTelegramChallengeStore redisChallengeStore;
    private final RiksaRedisProperties redisProperties;
    private final OtpHasher otpHasher;
    private final RedisRateLimiter rateLimiter;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PhoneVerificationService phoneVerification;

    // ---------------------------------------------------------------- app API

    public TelegramBotInfoResponse botInfo() {
        boolean available;
        try {
            requireAvailable();
            available = true;
        } catch (IllegalStateException e) {
            available = false;
        }
        if (!available) {
            return new TelegramBotInfoResponse(false, null, null, null, CODE_LENGTH);
        }
        String botUsername = normalizedBotUsername();
        return new TelegramBotInfoResponse(
                true,
                botUsername,
                "https://t.me/" + botUsername + "?start=code",
                "tg://resolve?domain=" + botUsername + "&start=code",
                CODE_LENGTH
        );
    }

    @Transactional
    public TelegramAuthStartResponse start() {
        requireAvailable();

        String requestToken = newRequestToken();
        String nonce = HexFormat.of().formatHex(randomBytes(16));
        OffsetDateTime expiresAt = OffsetDateTime.now()
                .plusMinutes(telegramConfig.getAuthRequestExpirationMinutes());

        TelegramAuthChallenge challenge = new TelegramAuthChallenge();
        challenge.setTokenHash(hash(requestToken));
        challenge.setStartNonceHash(linkNonceHash(nonce));
        challenge.setExpiresAt(expiresAt);
        saveChallenge(challenge);

        String botUsername = normalizedBotUsername();
        String payload = LOGIN_PAYLOAD_PREFIX + nonce;
        return new TelegramAuthStartResponse(
                requestToken,
                "https://t.me/" + botUsername + "?start=" + payload,
                "tg://resolve?domain=" + botUsername + "&start=" + payload,
                botUsername,
                expiresAt,
                Duration.ofMinutes(telegramConfig.getAuthRequestExpirationMinutes()).toSeconds(),
                POLL_INTERVAL_SECONDS
        );
    }

    @Transactional
    public TelegramAuthResult status(String requestToken) {
        TelegramAuthChallenge challenge = findByToken(requestToken)
                .orElseThrow(() -> new IllegalArgumentException("Telegram sign-in request was not found"));
        expireIfNeeded(challenge);
        return switch (challenge.getStatus()) {
            case PENDING -> TelegramAuthResult.pending(challenge.getExpiresAt());
            case DENIED -> TelegramAuthResult.of(TelegramAuthResult.Status.DENIED);
            case EXPIRED, COMPLETED -> TelegramAuthResult.of(TelegramAuthResult.Status.EXPIRED);
            case PROFILE_REQUIRED -> TelegramAuthResult.profileRequired(requestToken, challenge);
            case CONFIRMED -> resolveConfirmed(challenge, requestToken);
        };
    }

    @Transactional
    public TelegramAuthResult redeemCode(String rawCode) {
        TelegramAuthChallenge challenge = findConfirmedCode(rawCode);
        return resolveConfirmed(challenge, null);
    }

    @Transactional
    public AuthResponse completeSignup(TelegramAuthCompleteRequest request) {
        TelegramAuthChallenge challenge = findByToken(request.signupToken())
                .orElseThrow(TelegramAuthFlowService::signupExpired);
        expireIfNeeded(challenge);
        if (challenge.getStatus() != TelegramAuthChallengeStatus.PROFILE_REQUIRED) {
            throw signupExpired();
        }

        // Another device may have finished signup for this Telegram user meanwhile.
        Optional<User> existing = userRepository.findByTelegramId(challenge.getTelegramUserId());
        String username = request.username().trim();
        if (existing.isEmpty() && userRepository.existsByUsername(username)) {
            // Checked before claiming, so the user can simply pick another name.
            throw new IllegalStateException("Username is already taken");
        }

        claimOnce(challenge);
        User user = existing.orElseGet(() ->
                createTelegramUser(challenge, username, request.displayName().trim()));
        requireActive(user);
        complete(challenge);
        return buildAuthResponse(user);
    }

    @Transactional
    public void link(UUID userId, TelegramLinkRequest request) {
        TelegramAuthChallenge challenge;
        if (request.requestToken() != null && !request.requestToken().isBlank()) {
            challenge = findByToken(request.requestToken())
                    .orElseThrow(() -> new IllegalArgumentException("Telegram sign-in request was not found"));
            expireIfNeeded(challenge);
            if (challenge.getStatus() != TelegramAuthChallengeStatus.CONFIRMED) {
                throw new IllegalArgumentException("Confirm the request in Telegram before linking");
            }
        } else if (request.code() != null && !request.code().isBlank()) {
            challenge = findConfirmedCode(request.code());
        } else {
            throw new IllegalArgumentException("Provide a Telegram request token or sign-in code");
        }

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

        claimOnce(challenge);
        target.setTelegramId(challenge.getTelegramUserId());
        target.setTelegramChatId(challenge.getTelegramChatId());
        target.setPhoneVerified(true);
        userRepository.save(target);
        complete(challenge);
    }

    // ---------------------------------------------------------------- bot updates

    @Transactional
    public void handleWebhook(String providedSecret, TelegramUpdate update) {
        validateWebhookSecret(providedSecret);
        handleUpdate(update);
    }

    /** Entry point shared by the webhook and the local long-polling runner. */
    @Transactional
    public void handleUpdate(TelegramUpdate update) {
        if (update == null) return;
        if (update.callbackQuery() != null) {
            handleCallback(update.callbackQuery());
            return;
        }

        TelegramUpdate.TelegramMessage message = update.message();
        if (message == null || message.chat() == null || message.from() == null) {
            return;
        }
        if (!"private".equals(message.chat().type())
                || Boolean.TRUE.equals(message.from().bot())
                || !message.chat().id().equals(message.from().id())) {
            return;
        }
        if (message.contact() != null) {
            if (phoneVerification == null || !phoneVerification.handleContact(message)) {
                telegramService.sendMessage(message.chat().id().toString(), TelegramBotMessages.help());
            }
            return;
        }
        if (message.text() == null) {
            return;
        }

        String[] parts = message.text().trim().split("\\s+", 2);
        String command = parts[0].toLowerCase(Locale.ROOT);
        int mention = command.indexOf('@');
        if (mention > 0) command = command.substring(0, mention);

        switch (command) {
            case "/start" -> {
                if (parts.length == 2 && parts[1].startsWith(LOGIN_PAYLOAD_PREFIX)) {
                    handleLoginLink(message, parts[1].substring(LOGIN_PAYLOAD_PREFIX.length()));
                } else if (parts.length == 2 && phoneVerification != null
                        && parts[1].startsWith(PhoneVerificationService.PAYLOAD_PREFIX)) {
                    phoneVerification.handleStart(
                            message, parts[1].substring(PhoneVerificationService.PAYLOAD_PREFIX.length()));
                } else {
                    // First contact, or someone opening the bot directly: hand out a code.
                    sendLoginCode(message);
                }
            }
            case "/code", "/login" -> sendLoginCode(message);
            default -> telegramService.sendMessage(message.chat().id().toString(), TelegramBotMessages.help());
        }
    }

    private void handleLoginLink(TelegramUpdate.TelegramMessage message, String nonce) {
        String chatId = message.chat().id().toString();
        Optional<TelegramAuthChallenge> found = LINK_NONCE_PATTERN.matcher(nonce).matches()
                ? findByNonce(linkNonceHash(nonce))
                : Optional.empty();
        if (found.isEmpty()) {
            telegramService.sendMessage(chatId, TelegramBotMessages.linkExpired());
            return;
        }

        TelegramAuthChallenge challenge = found.get();
        expireIfNeeded(challenge);
        String telegramUserId = message.from().id().toString();
        boolean sameUser = telegramUserId.equals(challenge.getTelegramUserId());

        switch (challenge.getStatus()) {
            case PENDING -> {
                if (challenge.getTelegramUserId() != null && !sameUser) {
                    telegramService.sendMessage(chatId, TelegramBotMessages.linkOpenedByAnotherAccount());
                    return;
                }
                bindIdentity(challenge, message.from(), chatId);
                saveChallenge(challenge);

                long minutesLeft = Math.max(1,
                        (Duration.between(OffsetDateTime.now(), challenge.getExpiresAt()).toSeconds() + 59) / 60);
                telegramService.sendMessage(
                        chatId,
                        TelegramBotMessages.confirmPrompt(
                                challenge.getTelegramDisplayName(),
                                challenge.getTelegramUsername(),
                                userRepository.existsByTelegramId(telegramUserId),
                                minutesLeft),
                        TelegramService.InlineKeyboard.row(
                                TelegramService.InlineButton.callback("✅ Confirm sign-in", CALLBACK_CONFIRM + nonce),
                                TelegramService.InlineButton.callback("✖️ Not me", CALLBACK_DENY + nonce)));
            }
            case CONFIRMED, PROFILE_REQUIRED -> telegramService.sendMessage(chatId, sameUser
                    ? TelegramBotMessages.alreadyConfirmed()
                    : TelegramBotMessages.linkExpired());
            case DENIED -> telegramService.sendMessage(chatId, sameUser
                    ? TelegramBotMessages.denied()
                    : TelegramBotMessages.linkExpired());
            case COMPLETED, EXPIRED -> telegramService.sendMessage(chatId, TelegramBotMessages.linkExpired());
        }
    }

    private void handleCallback(TelegramUpdate.TelegramCallbackQuery query) {
        String data = query.data();
        boolean confirm = data != null && data.startsWith(CALLBACK_CONFIRM);
        boolean deny = data != null && data.startsWith(CALLBACK_DENY);
        if (!confirm && !deny) {
            telegramService.answerCallback(query.id(), null);
            return;
        }

        TelegramUpdate.TelegramMessage message = query.message();
        String chatId = message != null && message.chat() != null
                ? message.chat().id().toString()
                : query.from() == null ? null : query.from().id().toString();
        Long messageId = message == null ? null : message.messageId();

        String nonce = data.substring(CALLBACK_CONFIRM.length());
        Optional<TelegramAuthChallenge> found = LINK_NONCE_PATTERN.matcher(nonce).matches()
                ? findByNonce(linkNonceHash(nonce))
                : Optional.empty();
        if (found.isEmpty()) {
            telegramService.answerCallback(query.id(), "This request has expired");
            telegramService.editMessage(chatId, messageId, TelegramBotMessages.linkExpired());
            return;
        }

        TelegramAuthChallenge challenge = found.get();
        expireIfNeeded(challenge);
        if (query.from() == null || !query.from().id().toString().equals(challenge.getTelegramUserId())) {
            telegramService.answerCallback(query.id(), "This request belongs to another Telegram account");
            return;
        }
        if (challenge.getStatus() != TelegramAuthChallengeStatus.PENDING) {
            boolean confirmedAlready = challenge.getStatus() == TelegramAuthChallengeStatus.CONFIRMED
                    || challenge.getStatus() == TelegramAuthChallengeStatus.PROFILE_REQUIRED;
            telegramService.answerCallback(query.id(),
                    confirmedAlready ? "Already confirmed" : "This request is no longer valid");
            if (!confirmedAlready) {
                telegramService.editMessage(chatId, messageId, TelegramBotMessages.linkExpired());
            }
            return;
        }

        challenge.setStatus(confirm ? TelegramAuthChallengeStatus.CONFIRMED : TelegramAuthChallengeStatus.DENIED);
        saveChallenge(challenge);
        telegramService.answerCallback(query.id(), confirm ? "Confirmed ✅" : "Cancelled");
        telegramService.editMessage(chatId, messageId,
                confirm ? TelegramBotMessages.confirmed() : TelegramBotMessages.denied());
    }

    private void sendLoginCode(TelegramUpdate.TelegramMessage message) {
        String chatId = message.chat().id().toString();
        String telegramUserId = message.from().id().toString();

        RateLimitDecision decision;
        try {
            decision = rateLimiter.acquire("telegram-login-code", telegramUserId, CODES_PER_WINDOW, CODE_WINDOW);
        } catch (RuntimeException e) {
            telegramService.sendMessage(chatId, TelegramBotMessages.unavailable());
            return;
        }
        if (!decision.allowed()) {
            telegramService.sendMessage(chatId, TelegramBotMessages.tooManyCodes(decision.retryAfterSeconds()));
            return;
        }

        String code = newLoginCode();
        int minutes = telegramConfig.getLoginCodeExpirationMinutes();
        TelegramAuthChallenge challenge = new TelegramAuthChallenge();
        // The request token is never handed out for codes; a fresh one is issued on redemption.
        challenge.setTokenHash(hash(newRequestToken()));
        challenge.setStartNonceHash(codeHash(code));
        challenge.setStatus(TelegramAuthChallengeStatus.CONFIRMED);
        challenge.setExpiresAt(OffsetDateTime.now().plusMinutes(minutes));
        bindIdentity(challenge, message.from(), chatId);
        saveChallenge(challenge);

        String appUrl = telegramConfig.getAppUrl();
        telegramService.sendMessage(
                chatId,
                TelegramBotMessages.loginCode(
                        message.from().firstName(),
                        code,
                        minutes,
                        userRepository.existsByTelegramId(telegramUserId)),
                appUrl == null || !appUrl.startsWith("https://")
                        ? null
                        : TelegramService.InlineKeyboard.row(TelegramService.InlineButton.link("📲 Open Riksa", appUrl)));
    }

    // ---------------------------------------------------------------- shared steps

    /** Signs an existing account in, or moves a new Telegram user to the profile step. */
    private TelegramAuthResult resolveConfirmed(TelegramAuthChallenge challenge, String knownRequestToken) {
        Optional<User> existing = userRepository.findByTelegramId(challenge.getTelegramUserId());
        if (existing.isPresent()) {
            User user = existing.get();
            requireActive(user);
            claimOnce(challenge);
            user.setTelegramChatId(challenge.getTelegramChatId());
            user.setPhoneVerified(true);
            user = userRepository.save(user);
            complete(challenge);
            return TelegramAuthResult.authenticated(buildAuthResponse(user));
        }

        TelegramAuthChallenge profileStep = challenge;
        String signupToken = knownRequestToken;
        if (signupToken == null) {
            // Codes have no token the app knows about, so issue one for the profile step.
            claimOnce(challenge);
            signupToken = newRequestToken();
            profileStep = new TelegramAuthChallenge();
            profileStep.setTokenHash(hash(signupToken));
            profileStep.setTelegramUserId(challenge.getTelegramUserId());
            profileStep.setTelegramChatId(challenge.getTelegramChatId());
            profileStep.setTelegramUsername(challenge.getTelegramUsername());
            profileStep.setTelegramDisplayName(challenge.getTelegramDisplayName());
            complete(challenge);
        }
        profileStep.setStatus(TelegramAuthChallengeStatus.PROFILE_REQUIRED);
        profileStep.setExpiresAt(OffsetDateTime.now().plus(PROFILE_WINDOW));
        saveChallenge(profileStep);
        return TelegramAuthResult.profileRequired(signupToken, profileStep);
    }

    private TelegramAuthChallenge findConfirmedCode(String rawCode) {
        String code = normalizeCode(rawCode);
        TelegramAuthChallenge challenge = code == null ? null : findByNonce(codeHash(code)).orElse(null);
        if (challenge != null) expireIfNeeded(challenge);
        if (challenge == null || challenge.getStatus() != TelegramAuthChallengeStatus.CONFIRMED) {
            throw new IllegalArgumentException("This code is invalid or has expired. Send /code to the bot for a new one.");
        }
        return challenge;
    }

    private User createTelegramUser(TelegramAuthChallenge challenge, String username, String displayName) {
        User user = new User();
        user.setTelegramId(challenge.getTelegramUserId());
        user.setTelegramChatId(challenge.getTelegramChatId());
        user.setUsername(username);
        user.setDisplayName(displayName);
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

    private void bindIdentity(TelegramAuthChallenge challenge, TelegramUpdate.TelegramUser from, String chatId) {
        challenge.setTelegramUserId(from.id().toString());
        challenge.setTelegramChatId(chatId);
        challenge.setTelegramUsername(trimTo(from.username(), 64));
        String firstName = from.firstName() == null ? "" : from.firstName().trim();
        String lastName = from.lastName() == null ? "" : from.lastName().trim();
        challenge.setTelegramDisplayName(trimTo((firstName + " " + lastName).trim(), 100));
    }

    private void complete(TelegramAuthChallenge challenge) {
        challenge.setStatus(TelegramAuthChallengeStatus.COMPLETED);
        challenge.setCompletedAt(OffsetDateTime.now());
        saveChallenge(challenge);
    }

    /** Guarantees tokens are issued at most once per challenge when Redis (no row lock) holds it. */
    private void claimOnce(TelegramAuthChallenge challenge) {
        if (!redisProperties.isOtpEnabled() || challenge.getId() != null) return;
        boolean claimed;
        try {
            claimed = redisChallengeStore.claim(challenge.getTokenHash(), CLAIM_TTL);
        } catch (RuntimeException e) {
            throw new ServiceUnavailableException("Telegram authentication is temporarily unavailable", e);
        }
        if (!claimed) {
            throw new GoneException("This Telegram sign-in was already used. Please start again.");
        }
    }

    private void expireIfNeeded(TelegramAuthChallenge challenge) {
        if (challenge.getStatus() != TelegramAuthChallengeStatus.COMPLETED
                && challenge.getStatus() != TelegramAuthChallengeStatus.EXPIRED
                && challenge.getExpiresAt().isBefore(OffsetDateTime.now())) {
            challenge.setStatus(TelegramAuthChallengeStatus.EXPIRED);
            saveChallenge(challenge);
        }
    }

    // ---------------------------------------------------------------- storage

    private Optional<TelegramAuthChallenge> findByToken(String requestToken) {
        if (requestToken == null || !REQUEST_TOKEN_PATTERN.matcher(requestToken).matches()) {
            throw new IllegalArgumentException("Telegram request token is invalid");
        }
        String tokenHash = hash(requestToken);
        if (redisProperties.isOtpEnabled()) {
            Optional<TelegramAuthChallenge> redisChallenge = redis(() -> redisChallengeStore.find(tokenHash));
            if (redisChallenge.isPresent()) return redisChallenge;
        }
        return challengeRepository.findByTokenHashForUpdate(tokenHash);
    }

    private Optional<TelegramAuthChallenge> findByNonce(String nonceHash) {
        if (redisProperties.isOtpEnabled()) {
            Optional<TelegramAuthChallenge> redisChallenge = redis(() -> redisChallengeStore.findByNonce(nonceHash));
            if (redisChallenge.isPresent()) return redisChallenge;
        }
        return challengeRepository.findByStartNonceHashForUpdate(nonceHash);
    }

    private void saveChallenge(TelegramAuthChallenge challenge) {
        if (redisProperties.isOtpEnabled() && challenge.getId() == null) {
            redis(() -> {
                redisChallengeStore.save(challenge);
                return null;
            });
        } else {
            challengeRepository.save(challenge);
        }
    }

    private <T> T redis(java.util.function.Supplier<T> operation) {
        try {
            return operation.get();
        } catch (RuntimeException e) {
            throw new ServiceUnavailableException("Telegram authentication is temporarily unavailable", e);
        }
    }

    // ---------------------------------------------------------------- helpers

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
                || !telegramConfig.receivesUpdates()) {
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

    private static GoneException signupExpired() {
        return new GoneException("Your Telegram approval expired. Please sign in with Telegram again.");
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

    /** Upper-cases and drops spaces/dashes so "ve9b-lz8j" still works; null if it cannot be a code. */
    static String normalizeCode(String raw) {
        if (raw == null) return null;
        String code = raw.toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
        if (code.length() != CODE_LENGTH) return null;
        for (int i = 0; i < code.length(); i++) {
            if (CODE_ALPHABET.indexOf(code.charAt(i)) < 0) return null;
        }
        return code;
    }

    private String newLoginCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private String linkNonceHash(String nonce) {
        return otpHasher.hash("telegram-link", nonce);
    }

    private String codeHash(String code) {
        return otpHasher.hash("telegram-code", code);
    }

    private String randomAvatar() {
        var avatars = AvatarCatalog.availableAvatars();
        return avatars.get(RANDOM.nextInt(avatars.size()));
    }

    private String newRequestToken() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(32));
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
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
