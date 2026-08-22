package com.riksa.auth.otp;

import com.riksa.common.redis.RedisKeyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisOtpStore {

    private static final DefaultRedisScript<Long> SAVE_SCRIPT = new DefaultRedisScript<>("""
            redis.call('HSET', KEYS[1], 'codeHash', ARGV[1], 'attempts', '0')
            redis.call('PEXPIRE', KEYS[1], ARGV[2])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> VERIFY_SCRIPT = new DefaultRedisScript<>("""
            local stored = redis.call('HGET', KEYS[1], 'codeHash')
            if not stored then return 0 end
            local attempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
            if stored == ARGV[1] then
                redis.call('DEL', KEYS[1])
                return 1
            end
            if attempts >= tonumber(ARGV[2]) then
                redis.call('DEL', KEYS[1])
                return -2
            end
            return -1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyFactory keyFactory;

    public void save(String identifier, OtpChannel channel, String codeHash, Duration ttl) {
        Long result = redisTemplate.execute(
                SAVE_SCRIPT,
                List.of(key(identifier, channel)),
                codeHash,
                Long.toString(ttl.toMillis())
        );
        if (result == null || result != 1L) throw new IllegalStateException("Unable to save OTP");
    }

    public OtpVerificationResult verify(
            String identifier,
            OtpChannel channel,
            String candidateHash,
            int maxAttempts
    ) {
        Long result = redisTemplate.execute(
                VERIFY_SCRIPT,
                List.of(key(identifier, channel)),
                candidateHash,
                Integer.toString(maxAttempts)
        );
        if (result == null) {
            throw new IllegalStateException("Redis OTP verification returned no result");
        }
        return switch (result.intValue()) {
            case 1 -> OtpVerificationResult.VERIFIED;
            case -1 -> OtpVerificationResult.INVALID;
            case -2 -> OtpVerificationResult.TOO_MANY_ATTEMPTS;
            default -> OtpVerificationResult.NOT_FOUND;
        };
    }

    private String key(String identifier, OtpChannel channel) {
        return keyFactory.key("otp", "v1", channel.name().toLowerCase(), keyFactory.hash(identifier));
    }
}
