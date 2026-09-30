package com.riksa.common.cache;

import com.riksa.common.config.RiksaRedisProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisCacheFeatureSwitch {

    private final RiksaRedisProperties properties;

    public boolean enabled() {
        return properties.isCacheEnabled();
    }
}
