package com.riksa.admin.service;

import com.riksa.admin.dto.AnnouncementRequest;
import com.riksa.admin.entity.Announcement;
import com.riksa.admin.repository.AnnouncementRepository;
import com.riksa.common.audit.AuditLogService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementAdminServiceTest {

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private AuditLogService auditLogService;
    @InjectMocks private AnnouncementAdminService announcementAdminService;

    @Test
    void listDerivesScheduledLiveAndEndedStatuses() {
        OffsetDateTime now = OffsetDateTime.now();
        when(announcementRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                announcement(now.plusDays(1), null),
                announcement(now.minusDays(1), now.plusDays(1)),
                announcement(now.minusDays(2), now.minusMinutes(1))));

        var result = announcementAdminService.list();

        assertThat(result).extracting(com.riksa.admin.dto.AnnouncementResponse::status)
                .containsExactly("SCHEDULED", "LIVE", "ENDED");
        verify(announcementRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void createRejectsEndTimeThatIsNotAfterStartAndRequestValidationReportsIt() {
        OffsetDateTime start = OffsetDateTime.now();
        AnnouncementRequest invalid = new AnnouncementRequest("Notice", "Body", start, start);

        assertThatThrownBy(() -> announcementAdminService.create(UUID.randomUUID(), invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endsAt must be after startsAt");
        assertThat(Validation.buildDefaultValidatorFactory().getValidator().validate(invalid))
                .anySatisfy(violation -> assertThat(violation.getMessage())
                        .isEqualTo("endsAt must be after startsAt"));
        verify(announcementRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void createUpdateAndDeleteWriteAuditRecords() {
        UUID actorId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.now().minusMinutes(1);
        AnnouncementRequest request = new AnnouncementRequest("Notice", "Body", start, null);
        Announcement saved = announcement(start, null);
        saved.setId(id);
        saved.setUpdatedAt(OffsetDateTime.now());
        when(announcementRepository.save(any(Announcement.class))).thenReturn(saved);
        when(announcementRepository.findById(id)).thenReturn(Optional.of(saved));

        announcementAdminService.create(actorId, request);
        announcementAdminService.update(actorId, id, request);
        announcementAdminService.delete(actorId, id);

        verify(auditLogService).record(eq(actorId), eq("ANNOUNCEMENT_CREATE"), eq("ANNOUNCEMENT"), eq(id), any());
        verify(auditLogService).record(eq(actorId), eq("ANNOUNCEMENT_UPDATE"), eq("ANNOUNCEMENT"), eq(id), any());
        verify(auditLogService).record(eq(actorId), eq("ANNOUNCEMENT_DELETE"), eq("ANNOUNCEMENT"), eq(id), any());
        verify(announcementRepository).delete(saved);
    }

    private Announcement announcement(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        Announcement announcement = new Announcement();
        announcement.setId(UUID.randomUUID());
        announcement.setTitle("Notice");
        announcement.setBody("Body");
        announcement.setStartsAt(startsAt);
        announcement.setEndsAt(endsAt);
        announcement.setUpdatedAt(OffsetDateTime.now());
        return announcement;
    }
}
