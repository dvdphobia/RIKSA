package com.riksa.common.redis;

import com.riksa.common.config.RiksaRedisProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class RedisKeyFactory {

    private final String prefix;

    public RedisKeyFactory(RiksaRedisProperties properties) {
        this.prefix = trimColons(properties.getPrefix());
    }

    public String key(String... parts) {
        String suffix = Stream.of(parts)
                .map(RedisKeyFactory::trimColons)
                .collect(Collectors.joining(":"));
        return prefix + ":" + suffix;
    }

    public String hash(String sensitiveValue) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sensitiveValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String trimColons(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Redis key segment must not be blank");
        }
        return value.replaceAll("^:+|:+$", "");
    }
}
