package com.riksa.leaderboard.service;

import com.riksa.common.config.RiksaRedisProperties;
import com.riksa.common.enums.ScopeType;
import com.riksa.common.enums.TierType;
import com.riksa.common.redis.RedisKeyFactory;
import com.riksa.leaderboard.entity.LeaderboardSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisLeaderboardStore {

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyFactory keyFactory;
    private final RiksaRedisProperties properties;

    public boolean isEnabledAndReady() {
        if (!properties.isLeaderboardEnabled()) return false;
        return LocalDate.now().toString().equals(redisTemplate.opsForValue().get(readyKey()));
    }

    public void replaceAll(List<LeaderboardSnapshot> snapshots) {
        if (!properties.isLeaderboardEnabled()) return;

        Set<String> oldBoards = redisTemplate.opsForSet().members(boardsKey());
        if (oldBoards != null && !oldBoards.isEmpty()) redisTemplate.delete(oldBoards);
        redisTemplate.delete(List.of(boardsKey(), metadataKey(), readyKey(), userProvinceKey()));

        Map<String, List<LeaderboardSnapshot>> grouped = new HashMap<>();
        for (LeaderboardSnapshot snapshot : snapshots) {
            grouped.computeIfAbsent(boardKey(snapshot.getScope(), snapshot.getScopeId()), ignored -> new ArrayList<>())
                    .add(snapshot);
            redisTemplate.opsForHash().put(
                    metadataKey(), snapshot.getUserId().toString(), snapshot.getRankingTier().name());
            if (snapshot.getScope() == ScopeType.PROVINCE) {
                redisTemplate.opsForHash().put(
                        userProvinceKey(), snapshot.getUserId().toString(), snapshot.getScopeId());
            }
        }

        for (Map.Entry<String, List<LeaderboardSnapshot>> entry : grouped.entrySet()) {
            for (LeaderboardSnapshot snapshot : entry.getValue()) {
                redisTemplate.opsForZSet().add(
                        entry.getKey(), snapshot.getUserId().toString(), snapshot.getScore());
            }
            redisTemplate.opsForSet().add(boardsKey(), entry.getKey());
        }
        redisTemplate.opsForValue().set(readyKey(), LocalDate.now().toString());
    }

    public boolean isEnabled() {
        return properties.isLeaderboardEnabled();
    }

    public void removeUser(UUID userId) {
        if (!properties.isLeaderboardEnabled()) return;

        String member = userId.toString();
        redisTemplate.opsForZSet().remove(boardKey(ScopeType.COUNTRY, "CAMBODIA"), member);
        Object provinceId = redisTemplate.opsForHash().get(userProvinceKey(), member);
        if (provinceId != null) {
            redisTemplate.opsForZSet().remove(boardKey(ScopeType.PROVINCE, provinceId.toString()), member);
        }
        redisTemplate.opsForHash().delete(metadataKey(), member);
        redisTemplate.opsForHash().delete(userProvinceKey(), member);
    }

    public void updateUser(UUID userId, int score, TierType tier, String provinceId) {
        if (!properties.isLeaderboardEnabled()) return;

        String member = userId.toString();
        String countryBoard = boardKey(ScopeType.COUNTRY, "CAMBODIA");
        redisTemplate.opsForZSet().add(countryBoard, member, score);
        redisTemplate.opsForSet().add(boardsKey(), countryBoard);
        redisTemplate.opsForHash().put(metadataKey(), member, tier.name());

        Object previousProvince = redisTemplate.opsForHash().get(userProvinceKey(), member);
        if (previousProvince != null && !previousProvince.toString().equals(provinceId)) {
            redisTemplate.opsForZSet().remove(
                    boardKey(ScopeType.PROVINCE, previousProvince.toString()), member);
        }

        if (provinceId != null) {
            String provinceBoard = boardKey(ScopeType.PROVINCE, provinceId);
            redisTemplate.opsForZSet().add(provinceBoard, member, score);
            redisTemplate.opsForSet().add(boardsKey(), provinceBoard);
            redisTemplate.opsForHash().put(userProvinceKey(), member, provinceId);
        } else {
            redisTemplate.opsForHash().delete(userProvinceKey(), member);
        }
    }

    public List<LeaderboardSnapshot> getLeaderboard(ScopeType scope, String scopeId) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(boardKey(scope, scopeId), 0, -1);
        if (tuples == null || tuples.isEmpty()) {
            if (!isEnabledAndReady()) {
                throw new IllegalStateException("Redis leaderboard rebuild is in progress");
            }
            return List.of();
        }

        List<String> userIds = tuples.stream().map(ZSetOperations.TypedTuple::getValue).toList();
        List<Object> tiers = redisTemplate.opsForHash().multiGet(metadataKey(), new ArrayList<>(userIds));

        List<LeaderboardSnapshot> result = new ArrayList<>(tuples.size());
        int position = 0;
        int competitionRank = 0;
        Double previousScore = null;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            position++;
            if (previousScore == null || Double.compare(previousScore, tuple.getScore()) != 0) {
                competitionRank = position;
                previousScore = tuple.getScore();
            }
            LeaderboardSnapshot snapshot = new LeaderboardSnapshot();
            snapshot.setUserId(UUID.fromString(tuple.getValue()));
            snapshot.setScope(scope);
            snapshot.setScopeId(scopeId);
            snapshot.setRank(competitionRank);
            snapshot.setScore(tuple.getScore().intValue());
            Object tier = tiers.get(position - 1);
            snapshot.setRankingTier(tier == null ? TierType.BRONZE : TierType.valueOf(tier.toString()));
            snapshot.setSnapshotDate(LocalDate.now());
            result.add(snapshot);
        }
        return result;
    }

    public Optional<LeaderboardSnapshot> getMyRank(UUID userId, ScopeType scope, String scopeId) {
        String key = boardKey(scope, scopeId);
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            if (!isEnabledAndReady()) {
                throw new IllegalStateException("Redis leaderboard rebuild is in progress");
            }
            return Optional.empty();
        }

        Long higherScores = redisTemplate.opsForZSet().count(key, score + 0.5d, Double.POSITIVE_INFINITY);
        Object tier = redisTemplate.opsForHash().get(metadataKey(), userId.toString());
        LeaderboardSnapshot snapshot = new LeaderboardSnapshot();
        snapshot.setUserId(userId);
        snapshot.setScope(scope);
        snapshot.setScopeId(scopeId);
        snapshot.setRank(Math.toIntExact((higherScores == null ? 0 : higherScores) + 1));
        snapshot.setScore(score.intValue());
        snapshot.setRankingTier(tier == null ? TierType.BRONZE : TierType.valueOf(tier.toString()));
        snapshot.setSnapshotDate(LocalDate.now());
        return Optional.of(snapshot);
    }

    private String boardKey(ScopeType scope, String scopeId) {
        return keyFactory.key("lb", "v1", scope.name().toLowerCase(), scopeId);
    }

    private String boardsKey() {
        return keyFactory.key("lb", "v1", "boards");
    }

    private String metadataKey() {
        return keyFactory.key("lb", "v1", "tier");
    }

    private String userProvinceKey() {
        return keyFactory.key("lb", "v1", "user-province");
    }

    private String readyKey() {
        return keyFactory.key("lb", "v1", "ready");
    }
}
