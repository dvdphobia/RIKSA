package com.riksa.common.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AdminAuditLogRepository auditLogRepository;

    @InjectMocks private AuditLogService auditLogService;

    @Test
    void record_validInput_persistsEntry() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(auditLogRepository.save(any(AdminAuditLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        auditLogService.record(actorId, "USER_SUSPEND", "USER", targetId, Map.of("reason", "spam"));

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AdminAuditLog saved = captor.getValue();

        assertThat(saved.getActorId()).isEqualTo(actorId);
        assertThat(saved.getAction()).isEqualTo("USER_SUSPEND");
        assertThat(saved.getTargetType()).isEqualTo("USER");
        assertThat(saved.getTargetId()).isEqualTo(targetId);
        assertThat(saved.getDetails()).containsEntry("reason", "spam");
    }

    @Test
    void record_nullTargetAndDetails_persistsEntry() {
        UUID actorId = UUID.randomUUID();
        when(auditLogRepository.save(any(AdminAuditLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        auditLogService.record(actorId, "CONFIG_EXPORT", "SYSTEM", null, null);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetId()).isNull();
        assertThat(captor.getValue().getDetails()).isNull();
    }

    @Test
    void record_repositoryThrows_doesNotPropagate() {
        when(auditLogRepository.save(any(AdminAuditLog.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> auditLogService.record(
                UUID.randomUUID(), "POST_HIDE", "POST", UUID.randomUUID(), Map.of("reason", "nsfw")))
                .doesNotThrowAnyException();
    }

    @Test
    void search_delegatesToRepositoryAndMapsToDto() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        AdminAuditLog entity = new AdminAuditLog();
        entity.setId(UUID.randomUUID());
        entity.setActorId(actorId);
        entity.setAction("XP_ADJUST");
        entity.setTargetType("XP");
        entity.setTargetId(targetId);
        entity.setDetails(Map.of("amount", 50));
        entity.setCreatedAt(now);

        when(auditLogRepository.findAll(any(Example.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        Page<AuditLogResponse> result = auditLogService.search("XP", targetId, actorId, "XP_ADJUST", 0, 20);

        assertThat(result.getContent()).hasSize(1);
        AuditLogResponse dto = result.getContent().get(0);
        assertThat(dto.getId()).isEqualTo(entity.getId());
        assertThat(dto.getActorId()).isEqualTo(actorId);
        assertThat(dto.getAction()).isEqualTo("XP_ADJUST");
        assertThat(dto.getTargetType()).isEqualTo("XP");
        assertThat(dto.getTargetId()).isEqualTo(targetId);
        assertThat(dto.getDetails()).containsEntry("amount", 50);
        assertThat(dto.getCreatedAt()).isEqualTo(now);

        ArgumentCaptor<Example<AdminAuditLog>> exampleCaptor = ArgumentCaptor.forClass(Example.class);
        verify(auditLogRepository).findAll(exampleCaptor.capture(), eq(PageRequest.of(0, 20,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))));
        AdminAuditLog probe = exampleCaptor.getValue().getProbe();
        assertThat(probe.getTargetType()).isEqualTo("XP");
        assertThat(probe.getTargetId()).isEqualTo(targetId);
        assertThat(probe.getActorId()).isEqualTo(actorId);
        assertThat(probe.getAction()).isEqualTo("XP_ADJUST");
    }

    @Test
    void search_nullFilters_passesNullsThrough() {
        when(auditLogRepository.findAll(any(Example.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<AuditLogResponse> result = auditLogService.search(null, null, null, null, 1, 10);

        assertThat(result.getContent()).isEmpty();
        ArgumentCaptor<Example<AdminAuditLog>> exampleCaptor = ArgumentCaptor.forClass(Example.class);
        verify(auditLogRepository).findAll(exampleCaptor.capture(), eq(PageRequest.of(1, 10,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))));
        AdminAuditLog probe = exampleCaptor.getValue().getProbe();
        assertThat(probe.getTargetType()).isNull();
        assertThat(probe.getTargetId()).isNull();
        assertThat(probe.getActorId()).isNull();
        assertThat(probe.getAction()).isNull();
    }
}
