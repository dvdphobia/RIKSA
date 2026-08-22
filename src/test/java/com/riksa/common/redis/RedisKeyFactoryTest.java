package com.riksa.common.redis;

import com.riksa.common.config.RiksaRedisProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisKeyFactoryTest {

    @Test
    void buildsNamespacedKeysAndHashesSensitiveValues() {
        RiksaRedisProperties properties = new RiksaRedisProperties();
        properties.setPrefix("riksa:test:");
        RedisKeyFactory factory = new RedisKeyFactory(properties);

        assertThat(factory.key("otp", "v1", "telegram")).isEqualTo("riksa:test:otp:v1:telegram");
        assertThat(factory.hash("user@example.com"))
                .hasSize(64)
                .doesNotContain("user@example.com")
                .isEqualTo(factory.hash("user@example.com"));
    }
}
