package com.riksa.auth.security;

import com.riksa.common.config.RiksaRedisProperties;
import com.riksa.common.redis.RedisKeyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyFactory keyFactory;
    private final RiksaRedisProperties properties;

    public RateLimitDecision acquire(String bucket, String clientKey, long limit, Duration window) {
        if (!properties.isRateLimitEnabled()) {
            return new RateLimitDecision(true, limit, limit, 0);
        }

        String redisKey = keyFactory.key("rl", "v1", bucket, keyFactory.hash(clientKey));
        Long count = redisTemplate.execute(
                INCREMENT_SCRIPT,
                List.of(redisKey),
                Long.toString(window.toMillis())
        );
        if (count == null) {
            throw new IllegalStateException("Redis rate-limit script returned no result");
        }

        Long ttlMillis = redisTemplate.getExpire(redisKey, TimeUnit.MILLISECONDS);
        long retryAfter = ttlMillis == null || ttlMillis < 0
                ? Math.max(1, window.toSeconds())
                : Math.max(1, (ttlMillis + 999) / 1000);
        return new RateLimitDecision(count <= limit, limit, Math.max(0, limit - count), retryAfter);
    }
}
