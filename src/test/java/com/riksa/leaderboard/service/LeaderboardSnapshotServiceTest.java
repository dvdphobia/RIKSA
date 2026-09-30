package com.riksa.leaderboard.service;

import com.riksa.leaderboard.entity.LeaderboardSnapshot;
import com.riksa.leaderboard.repository.LeaderboardSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class LeaderboardSnapshotServiceTest {

    @Mock private LeaderboardSnapshotRepository snapshotRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @InjectMocks private LeaderboardSnapshotService snapshotService;

    @Test
    void refreshSnapshots_excludesHiddenAndSuspendedUsersFromBothScopes() {
        doAnswer(invocation -> Collections.emptyList())
                .when(jdbcTemplate).query(anyString(), ArgumentMatchers.<RowMapper<LeaderboardSnapshot>>any());

        var counts = snapshotService.refreshSnapshots();

        assertThat(counts.countryRows()).isZero();
        assertThat(counts.provinceRows()).isZero();
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).query(sql.capture(), ArgumentMatchers.<RowMapper<LeaderboardSnapshot>>any());
        assertThat(sql.getAllValues()).hasSize(2).allSatisfy(query -> {
            assertThat(query).contains("leaderboard_excluded = FALSE");
            assertThat(query).contains("suspended_until IS NULL OR u.suspended_until <= now()");
        });
    }
}
