package com.riksa.leaderboard.service;

import com.riksa.common.enums.ScopeType;
import com.riksa.common.enums.TierType;
import com.riksa.leaderboard.entity.LeaderboardSnapshot;
import com.riksa.leaderboard.repository.LeaderboardSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardSnapshotService {

    private final LeaderboardSnapshotRepository snapshotRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void refreshSnapshots() {
        LocalDate today = LocalDate.now();
        log.info("Refreshing leaderboard snapshots for {}", today);

        // COUNTRY scope
        List<LeaderboardSnapshot> countrySnapshots = jdbcTemplate.query("""
            SELECT
                ug.user_id,
                (ug.total_xp + (ug.current_streak * 5)) AS score,
                RANK() OVER (ORDER BY (ug.total_xp + (ug.current_streak * 5)) DESC) AS rank,
                rt.tier
            FROM user_gamification ug
            JOIN rank_tiers rt ON rt.min_xp <= ug.total_xp
            ORDER BY rt.min_xp DESC
        """, (rs, rowNum) -> {
            LeaderboardSnapshot s = new LeaderboardSnapshot();
            s.setUserId(UUID.fromString(rs.getString("user_id")));
            s.setScore(rs.getInt("score"));
            s.setRank(rs.getInt("rank"));
            s.setScope(ScopeType.COUNTRY);
            s.setScopeId("CAMBODIA");
            s.setRankingTier(TierType.valueOf(rs.getString("tier")));
            s.setSnapshotDate(today);
            return s;
        });

        snapshotRepository.saveAll(countrySnapshots);

        // PROVINCE scope
        List<LeaderboardSnapshot> provinceSnapshots = jdbcTemplate.query("""
            SELECT
                u.id AS user_id,
                u.province_id,
                (ug.total_xp + (ug.current_streak * 5)) AS score,
                RANK() OVER (PARTITION BY u.province_id ORDER BY (ug.total_xp + (ug.current_streak * 5)) DESC) AS rank,
                rt.tier
            FROM users u
            JOIN user_gamification ug ON ug.user_id = u.id
            JOIN rank_tiers rt ON rt.min_xp <= ug.total_xp
            WHERE u.province_id IS NOT NULL
            ORDER BY u.province_id, rt.min_xp DESC
        """, (rs, rowNum) -> {
            LeaderboardSnapshot s = new LeaderboardSnapshot();
            s.setUserId(UUID.fromString(rs.getString("user_id")));
            s.setScore(rs.getInt("score"));
            s.setRank(rs.getInt("rank"));
            s.setScope(ScopeType.PROVINCE);
            s.setScopeId(rs.getString("province_id"));
            s.setRankingTier(TierType.valueOf(rs.getString("tier")));
            s.setSnapshotDate(today);
            return s;
        });

        snapshotRepository.saveAll(provinceSnapshots);
        log.info("Leaderboard refresh done — {} country + {} province rows",
                countrySnapshots.size(), provinceSnapshots.size());
    }
}