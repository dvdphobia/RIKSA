package com.riksa.gamification.service;

import com.riksa.common.event.EventPublisher;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.repository.UserGamificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock UserGamificationRepository gamificationRepository;
    @Mock EventPublisher eventPublisher;

    private UserGamification gamification;
    private StreakService service;

    @BeforeEach
    void setUp() {
        gamification = new UserGamification();
        gamification.setUserId(USER_ID);
        gamification.setCurrentStreak(8);
        gamification.setHighestStreak(10);
        when(gamificationRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(gamification));
        service = new StreakService(gamificationRepository, eventPublisher);
    }

    @Test
    void firstCompletionStartsStreakAndRecordsCambodiaTime() {
        gamification.setCurrentStreak(0);
        gamification.setHighestStreak(0);

        long streak = service.updateStreak(USER_ID, OffsetDateTime.parse("2026-01-31T18:00:00Z"));

        assertThat(streak).isEqualTo(1);
        assertThat(gamification.getCurrentStreak()).isEqualTo(1);
        assertThat(gamification.getHighestStreak()).isEqualTo(1);
        assertThat(gamification.getLastCompletedAt())
                .isEqualTo(OffsetDateTime.parse("2026-02-01T01:00:00+07:00"));
        assertThat(gamification.getLastCompletedAt().getOffset()).isEqualTo(ZoneOffset.ofHours(7));
        verify(gamificationRepository).save(gamification);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-01-01T10:00:00+07:00, 2026-02-01T10:00:00+07:00, 1",
            "2025-12-01T10:00:00+07:00, 2026-01-01T10:00:00+07:00, 1",
            "2026-01-10T10:00:00+07:00, 2026-01-12T10:00:00+07:00, 1",
            "2026-01-10T10:00:00+07:00, 2026-01-11T10:00:00+07:00, 9",
            "2026-01-31T10:00:00+07:00, 2026-02-01T10:00:00+07:00, 9",
            "2025-12-31T10:00:00+07:00, 2026-01-01T10:00:00+07:00, 9",
            "2025-12-31T16:30:00Z, 2025-12-31T17:30:00Z, 9"
    })
    void countsActualDaysInCambodia(String lastCompletedAt, String completedAt, long expectedStreak) {
        gamification.setLastCompletedAt(OffsetDateTime.parse(lastCompletedAt));

        assertThat(service.updateStreak(USER_ID, OffsetDateTime.parse(completedAt))).isEqualTo(expectedStreak);
        assertThat(gamification.getCurrentStreak()).isEqualTo(expectedStreak);
        assertThat(gamification.getHighestStreak()).isEqualTo(10);
        assertThat(gamification.getLastCompletedAt()).isEqualTo(OffsetDateTime.parse(completedAt));
        verify(gamificationRepository).save(gamification);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-02-01T10:00:00+07:00, 2026-02-01T20:00:00+07:00",
            "2026-01-31T18:00:00Z, 2026-02-01T10:00:00Z",
            "2026-02-01T10:00:00+07:00, 2026-01-31T10:00:00+07:00",
            "2026-02-01T10:00:00+07:00, 2025-01-01T10:00:00+07:00"
    })
    void sameDayAndOlderEventsLeaveStreakAndCompletionTimeUnchanged(String lastCompletedAt, String completedAt) {
        OffsetDateTime lastCompletion = OffsetDateTime.parse(lastCompletedAt);
        gamification.setLastCompletedAt(lastCompletion);

        assertThat(service.updateStreak(USER_ID, OffsetDateTime.parse(completedAt))).isEqualTo(8);
        assertThat(gamification.getCurrentStreak()).isEqualTo(8);
        assertThat(gamification.getHighestStreak()).isEqualTo(10);
        assertThat(gamification.getLastCompletedAt()).isEqualTo(lastCompletion);
        verify(gamificationRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }
}
