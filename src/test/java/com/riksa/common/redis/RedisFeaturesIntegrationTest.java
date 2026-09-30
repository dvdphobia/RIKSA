package com.riksa.common.redis;

import com.riksa.auth.otp.OtpChannel;
import com.riksa.auth.otp.OtpHasher;
import com.riksa.auth.otp.OtpVerificationResult;
import com.riksa.auth.otp.RedisOtpStore;
import com.riksa.auth.security.RedisRateLimiter;
import com.riksa.common.config.RiksaRedisProperties;
import com.riksa.common.enums.ScopeType;
import com.riksa.common.enums.TierType;
import com.riksa.leaderboard.entity.LeaderboardSnapshot;
import com.riksa.leaderboard.service.RedisLeaderboardStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RedisFeaturesIntegrationTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:8.2-alpine"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RiksaRedisProperties properties;
    private static RedisKeyFactory keyFactory;

    @BeforeAll
    static void setUpRedis() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        properties = new RiksaRedisProperties();
        properties.setPrefix("riksa:test");
        properties.setRateLimitEnabled(true);
        properties.setOtpEnabled(true);
        properties.setLeaderboardEnabled(true);
        keyFactory = new RedisKeyFactory(properties);
    }

    @AfterEach
    void clearRedis() {
        try (var connection = connectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    @AfterAll
    static void closeRedis() {
        if (connectionFactory != null) connectionFactory.destroy();
    }

    @Test
    void rateLimit_isAtomicAndExpires() {
        RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate, keyFactory, properties);

        assertThat(limiter.acquire("login", "127.0.0.1", 2, Duration.ofSeconds(30)).allowed()).isTrue();
        assertThat(limiter.acquire("login", "127.0.0.1", 2, Duration.ofSeconds(30)).allowed()).isTrue();
        var blocked = limiter.acquire("login", "127.0.0.1", 2, Duration.ofSeconds(30));

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.remaining()).isZero();
        assertThat(blocked.retryAfterSeconds()).isBetween(1L, 30L);
    }

    @Test
    void otp_isSingleUseAndAttemptLimited() {
        RedisOtpStore store = new RedisOtpStore(redisTemplate, keyFactory);
        OtpHasher hasher = new OtpHasher("a-secure-test-secret-that-is-at-least-32-characters");
        String identifier = "telegram-123";
        String hash = hasher.hash(identifier, "123456");
        store.save(identifier, OtpChannel.TELEGRAM, hash, Duration.ofMinutes(5));

        assertThat(store.verify(identifier, OtpChannel.TELEGRAM,
                hasher.hash(identifier, "000000"), 3)).isEqualTo(OtpVerificationResult.INVALID);
        assertThat(store.verify(identifier, OtpChannel.TELEGRAM, hash, 3))
                .isEqualTo(OtpVerificationResult.VERIFIED);
        assertThat(store.verify(identifier, OtpChannel.TELEGRAM, hash, 3))
                .isEqualTo(OtpVerificationResult.NOT_FOUND);
    }

    @Test
    void leaderboard_preservesCompetitionRanksForTies() {
        RedisLeaderboardStore store = new RedisLeaderboardStore(redisTemplate, keyFactory, properties);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        store.replaceAll(List.of(
                snapshot(first, 100, TierType.SILVER),
                snapshot(second, 100, TierType.SILVER),
                snapshot(third, 50, TierType.BRONZE)
        ));

        var rows = store.getLeaderboard(ScopeType.COUNTRY, "CAMBODIA");

        assertThat(rows).extracting(LeaderboardSnapshot::getRank).containsExactly(1, 1, 3);
        assertThat(store.getMyRank(third, ScopeType.COUNTRY, "CAMBODIA"))
                .get().extracting(LeaderboardSnapshot::getRank).isEqualTo(3);
    }

    private LeaderboardSnapshot snapshot(UUID userId, int score, TierType tier) {
        LeaderboardSnapshot snapshot = new LeaderboardSnapshot();
        snapshot.setUserId(userId);
        snapshot.setScope(ScopeType.COUNTRY);
        snapshot.setScopeId("CAMBODIA");
        snapshot.setScore(score);
        snapshot.setRankingTier(tier);
        return snapshot;
    }
}
