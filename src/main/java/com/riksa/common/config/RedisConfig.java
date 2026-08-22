package com.riksa.common.config;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration(proxyBeanMethods = false)
@EnableCaching
@EnableSchedulerLock(defaultLockAtMostFor = "15m")
@EnableConfigurationProperties(RiksaRedisProperties.class)
@Slf4j
public class RedisConfig implements CachingConfigurer {

    @Bean
    public LockProvider lockProvider(
            RedisConnectionFactory connectionFactory,
            RiksaRedisProperties properties
    ) {
        return new RedisLockProvider(connectionFactory, properties.getPrefix());
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache read failed for {}; falling back to PostgreSQL: {}",
                        cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache write failed for {}; PostgreSQL remains authoritative: {}",
                        cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache eviction failed for {}; TTL bounds stale data: {}",
                        cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache clear failed for {}; TTL bounds stale data: {}",
                        cache.getName(), exception.getMessage());
            }
        };
    }
}
