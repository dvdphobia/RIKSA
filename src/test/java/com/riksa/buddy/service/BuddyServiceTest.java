package com.riksa.buddy.service;

import com.riksa.buddy.entity.Buddy;
import com.riksa.buddy.entity.BuddyCareEvent;
import com.riksa.buddy.repository.BuddyCareEventRepository;
import com.riksa.buddy.repository.BuddyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuddyServiceTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-24T10:00:00Z");

    @Mock BuddyRepository buddyRepository;
    @Mock BuddyCareEventRepository eventRepository;

    private Buddy buddy;
    private BuddyService service;

    @BeforeEach
    void setUp() {
        buddy = new Buddy();
        buddy.setUserId(USER_ID);
        buddy.setSpecies("crocodile");
        buddy.setName("Croco");
        buddy.setEnergy(80);
        buddy.setBond(30);
        buddy.setUpdatedAt(NOW);
        buddy.setCreatedAt(NOW);
        service = new BuddyService(
                buddyRepository,
                eventRepository,
                new BuddyCareRules(),
                Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
        when(buddyRepository.insertIfMissing(any(), anyString(), anyString(), any())).thenReturn(0);
        when(buddyRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(buddy));
        lenient().when(buddyRepository.save(any(Buddy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void duplicateSourceDoesNotAwardTwice() {
        UUID lessonId = UUID.randomUUID();
        when(eventRepository.existsByUserIdAndSourceKeyAndSourceId(USER_ID, "LESSON", lessonId))
                .thenReturn(true);

        service.recordCare(USER_ID, "LESSON", lessonId);

        assertThat(buddy.getEnergy()).isEqualTo(80);
        assertThat(buddy.getBond()).isEqualTo(30);
        verify(eventRepository, never()).save(any(BuddyCareEvent.class));
    }

    @Test
    void dailyPostCapStopsTheSecondPost() {
        UUID firstPost = UUID.randomUUID();
        UUID secondPost = UUID.randomUUID();
        when(eventRepository.existsByUserIdAndSourceKeyAndSourceId(USER_ID, "POST", firstPost))
                .thenReturn(false);
        when(eventRepository.existsByUserIdAndSourceKeyAndSourceId(USER_ID, "POST", secondPost))
                .thenReturn(false);
        when(eventRepository.countByUserIdAndSourceKeyAndCareDay(USER_ID, "POST", LocalDate.of(2026, 9, 24)))
                .thenReturn(0L, 1L);
        when(eventRepository.save(any(BuddyCareEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recordCare(USER_ID, "POST", firstPost);
        service.recordCare(USER_ID, "POST", secondPost);

        assertThat(buddy.getEnergy()).isEqualTo(90);
        assertThat(buddy.getBond()).isEqualTo(33);
        verify(eventRepository, times(1)).save(any(BuddyCareEvent.class));
    }

    @Test
    void lessonEnergyContinuesAfterTheDailyBondCap() {
        UUID lessonId = UUID.randomUUID();
        when(eventRepository.existsByUserIdAndSourceKeyAndSourceId(USER_ID, "LESSON", lessonId))
                .thenReturn(false);
        when(eventRepository.countByUserIdAndSourceKeyAndCareDay(USER_ID, "LESSON", LocalDate.of(2026, 9, 24)))
                .thenReturn(5L);
        when(eventRepository.save(any(BuddyCareEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recordCare(USER_ID, "LESSON", lessonId);

        assertThat(buddy.getEnergy()).isEqualTo(100);
        assertThat(buddy.getBond()).isEqualTo(30);
    }

    @Test
    void eventCountsUseThePhnomPenhCalendarDay() {
        Clock afterMidnight = Clock.fixed(
                OffsetDateTime.parse("2026-09-24T17:01:00Z").toInstant(), ZoneOffset.UTC);
        service = new BuddyService(buddyRepository, eventRepository, new BuddyCareRules(), afterMidnight);
        buddy.setUpdatedAt(OffsetDateTime.parse("2026-09-24T17:00:00Z"));
        UUID postId = UUID.randomUUID();
        when(eventRepository.existsByUserIdAndSourceKeyAndSourceId(USER_ID, "POST", postId))
                .thenReturn(false);
        when(eventRepository.countByUserIdAndSourceKeyAndCareDay(USER_ID, "POST", LocalDate.of(2026, 9, 25)))
                .thenReturn(0L);
        when(eventRepository.save(any(BuddyCareEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recordCare(USER_ID, "POST", postId);

        verify(eventRepository).countByUserIdAndSourceKeyAndCareDay(
                USER_ID, "POST", LocalDate.of(2026, 9, 25));
        assertThat(buddy.getEnergy()).isEqualTo(90);
    }

    @Test
    void guestMergeKeepsTheHigherServerAndGuestValues() {
        Buddy merged = service.mergeGuestState(USER_ID, "capybara", "Mochi", 95, 20);

        assertThat(merged.getEnergy()).isEqualTo(95);
        assertThat(merged.getBond()).isEqualTo(30);
        // Existing server identity wins during merge; explicit later updates
        // can still change species and name through PUT /buddy/me.
        assertThat(merged.getSpecies()).isEqualTo("crocodile");
        assertThat(merged.getName()).isEqualTo("Croco");
    }

    @Test
    void guestMergeAlsoKeepsHigherServerEnergyAndGuestBond() {
        Buddy merged = service.mergeGuestState(USER_ID, "capybara", "Mochi", 70, 45);

        assertThat(merged.getEnergy()).isEqualTo(80);
        assertThat(merged.getBond()).isEqualTo(45);
    }
}


