package com.riksa.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "riksa.redis")
public class RiksaRedisProperties {

    private String prefix = "riksa:dev";
    private boolean rateLimitEnabled = true;
    private boolean otpEnabled = true;
    private boolean cacheEnabled = true;
    private boolean leaderboardEnabled = true;
}
