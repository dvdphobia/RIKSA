package com.riksa.auth.telegram;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.security.RedisRateLimiter;
import com.riksa.common.redis.RedisKeyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhoneVerificationServiceTest {

    @Mock private TelegramConfig telegramConfig;
    @Mock private TelegramService telegramService;
    @Mock private UserRepository userRepository;
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> values;
    @Mock private RedisKeyFactory keyFactory;
    @Mock private RedisRateLimiter rateLimiter;

    private PhoneVerificationService service;
    private User user;
    private static final long TG_ID = 777L;

    @BeforeEach
    void setUp() {
        service = new PhoneVerificationService(
                telegramConfig, telegramService, userRepository, redis, keyFactory, rateLimiter);
        when(redis.opsForValue()).thenReturn(values);
        when(keyFactory.key(any(String[].class))).thenAnswer(inv -> String.join(":", (String[]) inv.getRawArguments()[0]));
        user = new User();
        user.setId(UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(values.get("phone:v1:chat:" + TG_ID)).thenReturn(user.getId().toString());
    }

    private static TelegramUpdate.TelegramMessage contact(String phone, Long ownerId) {
        return new TelegramUpdate.TelegramMessage(
                1L,
                new TelegramUpdate.TelegramChat(TG_ID, "private"),
                new TelegramUpdate.TelegramUser(TG_ID, false, "Sokha", null, "sokha"),
                null,
                new TelegramUpdate.TelegramContact(phone, "Sokha", ownerId));
    }

    @Test
    void ownContactVerifiesAndLinksTelegram() {
        assertThat(service.handleContact(contact("855 12 345 678", TG_ID))).isTrue();

        assertThat(user.getPhoneNumber()).isEqualTo("+85512345678");
        assertThat(user.getPhoneVerified()).isTrue();
        assertThat(user.getTelegramId()).isEqualTo(String.valueOf(TG_ID));
        verify(userRepository).save(user);
        verify(redis).delete("phone:v1:chat:" + TG_ID);
    }

    @Test
    void forwardedContactIsRejected() {
        service.handleContact(contact("85512345678", 999L));

        assertThat(user.getPhoneVerified()).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void numberUsedByAnotherAccountIsRejected() {
        User other = new User();
        other.setId(UUID.randomUUID());
        when(userRepository.findByPhoneNumber("+85512345678")).thenReturn(Optional.of(other));

        service.handleContact(contact("+85512345678", TG_ID));

        assertThat(user.getPhoneVerified()).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void contactWithoutAPendingVerificationIsIgnored() {
        when(values.get("phone:v1:chat:" + TG_ID)).thenReturn(null);

        assertThat(service.handleContact(contact("85512345678", TG_ID))).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void startRefusesATelegramAccountOtherThanTheLinkedOne() {
        user.setTelegramId("123");
        String nonce = "0123456789abcdef0123456789abcdef";
        when(values.getAndDelete("phone:v1:nonce:" + nonce)).thenReturn(user.getId().toString());

        service.handleStart(contact(null, null), nonce);

        verify(values, never()).set(eq("phone:v1:chat:" + TG_ID), anyString(), any());
        verify(telegramService).sendMessage(eq(String.valueOf(TG_ID)), contains("different Telegram"));
    }

    @Test
    void normalizesPhoneNumbers() {
        assertThat(PhoneVerificationService.normalize("+855 12-345-678")).isEqualTo("+85512345678");
        assertThat(PhoneVerificationService.normalize("123")).isNull();
        assertThat(PhoneVerificationService.mask("+85512345678")).isEqualTo("+855•••••678");
    }
}
