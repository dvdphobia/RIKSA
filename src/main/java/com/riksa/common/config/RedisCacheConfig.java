package com.riksa.common.config;

import com.riksa.common.cache.CacheNames;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class RedisCacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            RiksaRedisProperties properties
    ) {
        return builder -> builder
                .withCacheConfiguration(CacheNames.USER_SECURITY_STATE,
                        configuration(properties, CacheNames.USER_SECURITY_STATE, Duration.ofSeconds(30)))
                .withCacheConfiguration(CacheNames.GAMIFICATION_PROFILE,
                        configuration(properties, CacheNames.GAMIFICATION_PROFILE, Duration.ofSeconds(30)))
                .withCacheConfiguration(CacheNames.GAMIFICATION_ACHIEVEMENTS,
                        configuration(properties, CacheNames.GAMIFICATION_ACHIEVEMENTS, Duration.ofSeconds(30)))
                .withCacheConfiguration(CacheNames.GAMIFICATION_STREAK,
                        configuration(properties, CacheNames.GAMIFICATION_STREAK, Duration.ofSeconds(30)));
    }

    private RedisCacheConfiguration configuration(
            RiksaRedisProperties properties,
            String cacheName,
            Duration ttl
    ) {
        String prefix = properties.getPrefix() + ":cache:v1:" + cacheName + ":";
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(ttl)
                .computePrefixWith(ignored -> prefix)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        org.springframework.data.redis.serializer.RedisSerializer.json()));
    }
}
