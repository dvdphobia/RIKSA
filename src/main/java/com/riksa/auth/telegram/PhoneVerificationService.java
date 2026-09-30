package com.riksa.auth.telegram;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.security.RateLimitDecision;
import com.riksa.auth.security.RedisRateLimiter;
import com.riksa.common.exception.RateLimitExceededException;
import com.riksa.common.redis.RedisKeyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Verifies a phone number through Telegram, with no SMS.
 *
 * <ol>
 *   <li>The signed-in app calls {@link #start(UUID)} and opens
 *       {@code t.me/<bot>?start=phone_<nonce>}.</li>
 *   <li>The bot answers {@code /start phone_<nonce>} with a "Share my phone number" button.</li>
 *   <li>Telegram sends the user's own contact card, which Telegram has already verified;
 *       {@link #handleContact} saves the number and marks it verified.</li>
 * </ol>
 *
 * A contact card is accepted only when it belongs to the sender, so nobody can verify
 * with a number forwarded from someone else.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneVerificationService {

    static final String PAYLOAD_PREFIX = "phone_";
    static final Duration LINK_TTL = Duration.ofMinutes(15);
    private static final int STARTS_PER_WINDOW = 5;
    private static final Duration START_WINDOW = Duration.ofMinutes(10);
    private static final Pattern NONCE = Pattern.compile("[a-f0-9]{32}");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TelegramConfig telegramConfig;
    private final TelegramService telegramService;
    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final RedisKeyFactory keyFactory;
    private final RedisRateLimiter rateLimiter;

    public record StartResponse(String botUrl, String botAppUrl, long expiresInSeconds) {}

    // ── App ──────────────────────────────────────────────

    public StartResponse start(UUID userId) {
        requireAvailable();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Sign in first"));
        if (user.isSuspended()) {
            throw new AccessDeniedException("Account suspended until " + user.getSuspendedUntil());
        }
        RateLimitDecision decision = rateLimiter.acquire(
                "phone-verify-start", userId.toString(), STARTS_PER_WINDOW, START_WINDOW);
        if (!decision.allowed()) {
            throw new RateLimitExceededException(
                    "Too many tries. Please wait a few minutes and try again.", decision.retryAfterSeconds());
        }

        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String nonce = HexFormat.of().formatHex(bytes);
        redis.opsForValue().set(nonceKey(nonce), userId.toString(), LINK_TTL);

        String bot = botUsername();
        return new StartResponse(
                "https://t.me/" + bot + "?start=" + PAYLOAD_PREFIX + nonce,
                "tg://resolve?domain=" + bot + "&start=" + PAYLOAD_PREFIX + nonce,
                LINK_TTL.toSeconds());
    }

    // ── Bot ──────────────────────────────────────────────

    /** {@code /start phone_<nonce>}: ties this chat to the account and asks for the contact. */
    @Transactional(readOnly = true)
    public void handleStart(TelegramUpdate.TelegramMessage message, String nonce) {
        String chatId = message.chat().id().toString();
        String userId = NONCE.matcher(nonce).matches() ? redis.opsForValue().getAndDelete(nonceKey(nonce)) : null;
        Optional<User> user = userId == null ? Optional.empty() : userRepository.findById(UUID.fromString(userId));
        if (user.isEmpty()) {
            telegramService.sendMessage(chatId, TelegramBotMessages.phoneLinkExpired());
            return;
        }
        String telegramUserId = message.from().id().toString();
        String linked = user.get().getTelegramId();
        if (linked != null && !linked.equals(telegramUserId)) {
            telegramService.sendMessage(chatId, TelegramBotMessages.phoneWrongTelegram());
            return;
        }
        redis.opsForValue().set(chatKey(chatId), userId, LINK_TTL);
        telegramService.sendWithMarkup(chatId, TelegramBotMessages.phoneSharePrompt(),
                new TelegramService.ReplyKeyboard(
                        List.of(List.of(TelegramService.KeyboardButton.shareContact(
                                TelegramBotMessages.PHONE_SHARE_BUTTON))),
                        true, true));
    }

    /** A contact card arrived. Returns false when no verification is waiting in this chat. */
    @Transactional
    public boolean handleContact(TelegramUpdate.TelegramMessage message) {
        String chatId = message.chat().id().toString();
        String userId = redis.opsForValue().get(chatKey(chatId));
        if (userId == null) {
            return false;
        }
        TelegramUpdate.TelegramContact contact = message.contact();
        if (contact.userId() == null || !contact.userId().equals(message.from().id())) {
            telegramService.sendMessage(chatId, TelegramBotMessages.phoneNotOwnContact());
            return true;
        }
        String phone = normalize(contact.phoneNumber());
        if (phone == null) {
            telegramService.sendMessage(chatId, TelegramBotMessages.phoneNotOwnContact());
            return true;
        }

        User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
        if (user == null) {
            redis.delete(chatKey(chatId));
            telegramService.sendWithMarkup(chatId, TelegramBotMessages.phoneLinkExpired(),
                    TelegramService.ReplyKeyboardRemove.INSTANCE);
            return true;
        }
        String telegramUserId = message.from().id().toString();
        boolean numberTaken = userRepository.findByPhoneNumber(phone)
                .filter(other -> !other.getId().equals(user.getId()))
                .isPresent();
        boolean telegramTaken = userRepository.findByTelegramId(telegramUserId)
                .filter(other -> !other.getId().equals(user.getId()))
                .isPresent();
        if (numberTaken || telegramTaken) {
            redis.delete(chatKey(chatId));
            telegramService.sendWithMarkup(chatId, TelegramBotMessages.phoneTaken(),
                    TelegramService.ReplyKeyboardRemove.INSTANCE);
            return true;
        }

        user.setPhoneNumber(phone);
        user.setPhoneVerified(true);
        // Sharing a contact also proves this Telegram account is theirs.
        if (user.getTelegramId() == null) {
            user.setTelegramId(telegramUserId);
            if (userRepository.findByTelegramChatId(chatId).isEmpty()) {
                user.setTelegramChatId(chatId);
            }
        }
        userRepository.save(user);
        redis.delete(chatKey(chatId));
        telegramService.sendWithMarkup(chatId, TelegramBotMessages.phoneVerified(mask(phone)),
                TelegramService.ReplyKeyboardRemove.INSTANCE);
        log.info("Phone verified through Telegram for user {}", user.getId());
        return true;
    }

    // ── Helpers ──────────────────────────────────────────

    /** Telegram sends digits with or without a leading +; stored as +<digits>. */
    static String normalize(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() < 7 || digits.length() > 15) return null;
        return "+" + digits;
    }

    static String mask(String phone) {
        if (phone.length() <= 6) return phone;
        return phone.substring(0, 4) + "•".repeat(phone.length() - 7) + phone.substring(phone.length() - 3);
    }

    private void requireAvailable() {
        if (!telegramConfig.isEnabled()
                || telegramConfig.getBotToken() == null || telegramConfig.getBotToken().isBlank()
                || telegramConfig.getBotUsername() == null || telegramConfig.getBotUsername().isBlank()
                || !telegramConfig.receivesUpdates()) {
            throw new IllegalStateException("Phone verification through Telegram is not available right now");
        }
    }

    private String botUsername() {
        String username = telegramConfig.getBotUsername().trim();
        return username.startsWith("@") ? username.substring(1) : username;
    }

    private String nonceKey(String nonce) {
        return keyFactory.key("phone", "v1", "nonce", nonce);
    }

    private String chatKey(String chatId) {
        return keyFactory.key("phone", "v1", "chat", chatId);
    }
}
