package com.riksa.admin.service;

import com.riksa.admin.dto.AdminLessonResourceStatusRequest;
import com.riksa.common.audit.AuditLogService;
import com.riksa.resource.entity.LessonResource;
import com.riksa.resource.repository.LessonResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonResourceAdminServiceTest {

    private static final UUID RESOURCE_ID = UUID.randomUUID();
    private static final UUID LESSON_ID = UUID.randomUUID();
    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID MODERATOR_ID = UUID.randomUUID();

    @Mock private LessonResourceRepository resourceRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private LessonResourceAdminService service;

    @Test
    void listPassesHiddenFilterAndMapsPageToAdminRows() {
        var projection = projection(false, null, null);
        when(resourceRepository.searchAdmin(eq(LESSON_ID), eq(false), eq("paper"), eq(PageRequest.of(2, 50))))
                .thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(2, 50), 101));

        var page = service.list(LESSON_ID, false, " paper ", 2, 50);

        assertThat(page.getTotalElements()).isEqualTo(101);
        assertThat(page.getContent().getFirst().lessonTitle()).isEqualTo("Fractions");
        assertThat(page.getContent().getFirst().authorUsername()).isEqualTo("dara");
        assertThat(page.getContent().getFirst().saveCount()).isEqualTo(5L);
        assertThat(page.getContent().getFirst().hidden()).isFalse();
    }

    @Test
    void listCanFilterToHiddenRows() {
        var projection = projection(true, OffsetDateTime.parse("2026-09-25T10:00:00Z"), MODERATOR_ID);
        when(resourceRepository.searchAdmin(eq(null), eq(true), eq(""), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(projection)));

        var page = service.list(null, true, null, 0, 20);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().hidden()).isTrue();
        verify(resourceRepository).searchAdmin(null, true, "", PageRequest.of(0, 20));
    }

    @Test
    void hideSetsActorAndTimestampReturnsUpdatedResourceAndAuditsReason() {
        LessonResource resource = resource();
        when(resourceRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        var updatedProjection = projection(true, OffsetDateTime.parse("2026-09-25T10:00:00Z"), MODERATOR_ID);
        when(resourceRepository.findAdminById(RESOURCE_ID)).thenReturn(Optional.of(updatedProjection));
        AdminLessonResourceStatusRequest request = statusRequest(true, "outdated link");

        var response = service.updateStatus(MODERATOR_ID, RESOURCE_ID, request);

        assertThat(resource.getHiddenAt()).isNotNull();
        assertThat(resource.getHiddenBy()).isEqualTo(MODERATOR_ID);
        assertThat(response.hidden()).isTrue();
        assertThat(response.hiddenBy()).isEqualTo(MODERATOR_ID);
        verify(resourceRepository).saveAndFlush(resource);
        verify(auditLogService).record(MODERATOR_ID, "RESOURCE_HIDE", "LESSON_RESOURCE", RESOURCE_ID,
                Map.of("hidden", true, "reason", "outdated link"));
    }

    @Test
    void unhideClearsHiddenFieldsAndAudits() {
        LessonResource resource = resource();
        resource.setHiddenAt(OffsetDateTime.parse("2026-09-20T10:00:00Z"));
        resource.setHiddenBy(MODERATOR_ID);
        when(resourceRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        var updatedProjection = projection(false, null, null);
        when(resourceRepository.findAdminById(RESOURCE_ID)).thenReturn(Optional.of(updatedProjection));

        var response = service.updateStatus(MODERATOR_ID, RESOURCE_ID, statusRequest(false, null));

        assertThat(resource.getHiddenAt()).isNull();
        assertThat(resource.getHiddenBy()).isNull();
        assertThat(response.hidden()).isFalse();
        verify(auditLogService).record(MODERATOR_ID, "RESOURCE_UNHIDE", "LESSON_RESOURCE", RESOURCE_ID,
                Map.of("hidden", false));
    }

    @Test
    void deleteRemovesResourceSavesAndSaveRewardsAndAudits() {
        LessonResource resource = resource();
        when(resourceRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(resource));

        service.delete(MODERATOR_ID, RESOURCE_ID);

        verify(jdbcTemplate).update("DELETE FROM lesson_resource_save_reward WHERE resource_id = ?", RESOURCE_ID);
        verify(jdbcTemplate).update("DELETE FROM lesson_resource_save WHERE resource_id = ?", RESOURCE_ID);
        verify(resourceRepository).delete(resource);
        verify(auditLogService).record(MODERATOR_ID, "RESOURCE_DELETE", "LESSON_RESOURCE", RESOURCE_ID,
                Map.of("lessonId", LESSON_ID.toString(), "authorId", AUTHOR_ID.toString(), "title", "Practice paper"));
    }

    private static LessonResourceRepository.AdminLessonResourceProjection projection(
            boolean hidden, OffsetDateTime hiddenAt, UUID hiddenBy) {
        var row = mock(LessonResourceRepository.AdminLessonResourceProjection.class);
        when(row.getId()).thenReturn(RESOURCE_ID);
        when(row.getLessonId()).thenReturn(LESSON_ID);
        when(row.getLessonTitle()).thenReturn("Fractions");
        when(row.getAuthorId()).thenReturn(AUTHOR_ID);
        when(row.getAuthorUsername()).thenReturn("dara");
        when(row.getTitle()).thenReturn("Practice paper");
        when(row.getUrl()).thenReturn("https://example.edu/paper.pdf");
        when(row.getKind()).thenReturn("PDF");
        when(row.getHidden()).thenReturn(hidden);
        when(row.getHiddenAt()).thenReturn(hiddenAt);
        when(row.getHiddenBy()).thenReturn(hiddenBy);
        when(row.getSaveCount()).thenReturn(5L);
        when(row.getCreatedAt()).thenReturn(OffsetDateTime.parse("2026-09-18T10:00:00Z"));
        return row;
    }

    private static LessonResource resource() {
        LessonResource resource = new LessonResource();
        resource.setId(RESOURCE_ID);
        resource.setLessonId(LESSON_ID);
        resource.setAuthorId(AUTHOR_ID);
        resource.setTitle("Practice paper");
        resource.setUrl("https://example.edu/paper.pdf");
        resource.setCreatedAt(OffsetDateTime.parse("2026-09-18T10:00:00Z"));
        return resource;
    }

    private static AdminLessonResourceStatusRequest statusRequest(boolean hidden, String reason) {
        AdminLessonResourceStatusRequest request = new AdminLessonResourceStatusRequest();
        request.setHidden(hidden);
        request.setReason(reason);
        return request;
    }
}
