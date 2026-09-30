package com.riksa.auth.security;

import com.riksa.common.config.RiksaRedisProperties;
import com.riksa.common.redis.RedisKeyFactory;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisRateLimiterTest {

    @Test
    void disabledLimiter_allowsWithoutContactingRedis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RiksaRedisProperties properties = new RiksaRedisProperties();
        properties.setRateLimitEnabled(false);
        RedisRateLimiter limiter = new RedisRateLimiter(
                redisTemplate, new RedisKeyFactory(properties), properties);

        RateLimitDecision decision = limiter.acquire(
                "login", "127.0.0.1", 5, Duration.ofMinutes(1));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isEqualTo(5);
        verifyNoInteractions(redisTemplate);
    }
}
