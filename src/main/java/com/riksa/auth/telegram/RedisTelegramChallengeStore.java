package com.riksa.auth.telegram;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riksa.common.redis.RedisKeyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisTelegramChallengeStore {

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyFactory keyFactory;
    private final ObjectMapper objectMapper;

    public void save(TelegramAuthChallenge challenge) {
        Duration ttl = Duration.between(OffsetDateTime.now(), challenge.getExpiresAt());
        String key = key(challenge.getTokenHash());
        if (ttl.isNegative() || ttl.isZero()) {
            redisTemplate.delete(key);
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(challenge), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize Telegram auth challenge", e);
        }
        if (challenge.getStartNonceHash() != null) {
            redisTemplate.opsForValue().set(nonceKey(challenge.getStartNonceHash()), challenge.getTokenHash(), ttl);
        }
    }

    public Optional<TelegramAuthChallenge> findByNonce(String nonceHash) {
        String tokenHash = redisTemplate.opsForValue().get(nonceKey(nonceHash));
        return tokenHash == null ? Optional.empty() : find(tokenHash);
    }

    /**
     * Claims a one-time transition such as issuing tokens or creating the account.
     * Redis has no row lock, so concurrent polls race for this marker instead.
     */
    public boolean claim(String tokenHash, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(keyFactory.key("telegram", "v1", "claim", tokenHash), "1", ttl));
    }

    public Optional<TelegramAuthChallenge> find(String tokenHash) {
        String json = redisTemplate.opsForValue().get(key(tokenHash));
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, TelegramAuthChallenge.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to deserialize Telegram auth challenge", e);
        }
    }

    private String key(String tokenHash) {
        return keyFactory.key("telegram", "v1", "challenge", tokenHash);
    }

    private String nonceKey(String nonceHash) {
        return keyFactory.key("telegram", "v1", "nonce", nonceHash);
    }
}
