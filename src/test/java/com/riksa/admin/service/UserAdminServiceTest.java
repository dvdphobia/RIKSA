package com.riksa.admin.service;

import com.riksa.admin.dto.SuspensionRequest;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.dto.UserProfileResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private UserAdminService userAdminService;

    private final UUID actorId = UUID.randomUUID();

    @Test
    void updateRole_validModeratorRole_updatesRole() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("john");
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponse result = userAdminService.updateRole(actorId, userId, "MODERATOR");

        assertThat(result).isNotNull();
        assertThat(user.getRole()).isEqualTo(Role.MODERATOR);
    }

    @Test
    void updateRole_validAdminRole_updatesRole() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("admin_candidate");
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponse result = userAdminService.updateRole(actorId, userId, "ADMIN");

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void updateRole_setToUser_throwsException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.MODERATOR);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userAdminService.updateRole(actorId, userId, "USER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot set role to USER via admin endpoint");
    }

    @Test
    void updateRole_invalidRole_throwsException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userAdminService.updateRole(actorId, userId, "SUPERADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid role");
    }

    @Test
    void updateRole_nonExistentUser_throwsException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.updateRole(actorId, userId, "ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateRole_caseInsensitive_works() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userAdminService.updateRole(actorId, userId, "moderator");

        assertThat(user.getRole()).isEqualTo(Role.MODERATOR);
    }

    @Test
    void updateRole_recordsAuditEntry() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userAdminService.updateRole(actorId, userId, "MODERATOR");

        verify(auditLogService).record(eq(actorId), eq("USER_ROLE_CHANGE"), eq("USER"), eq(userId),
                eq(Map.of("oldRole", "USER", "newRole", "MODERATOR")));
    }

    @Test
    void suspend_validRequest_setsFieldsAndAudits() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime until = OffsetDateTime.now().plusDays(7);
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        SuspensionRequest request = new SuspensionRequest();
        request.setReason("spam");
        request.setUntil(until);

        UserProfileResponse result = userAdminService.suspend(actorId, userId, request);

        assertThat(result).isNotNull();
        assertThat(user.getSuspendedUntil()).isEqualTo(until);
        assertThat(user.getSuspensionReason()).isEqualTo("spam");

        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).record(eq(actorId), eq("USER_SUSPEND"), eq("USER"), eq(userId), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue())
                .containsEntry("reason", "spam")
                .containsEntry("until", until.toString());
    }

    @Test
    void suspend_moderatorTarget_throwsException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.MODERATOR);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        SuspensionRequest request = new SuspensionRequest();
        request.setReason("abuse");
        request.setUntil(OffsetDateTime.now().plusDays(1));

        assertThatThrownBy(() -> userAdminService.suspend(actorId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot suspend staff accounts");

        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void suspend_adminTarget_throwsException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        SuspensionRequest request = new SuspensionRequest();
        request.setReason("abuse");
        request.setUntil(OffsetDateTime.now().plusDays(1));

        assertThatThrownBy(() -> userAdminService.suspend(actorId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot suspend staff accounts");
    }

    @Test
    void suspend_pastUntil_throwsException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        SuspensionRequest request = new SuspensionRequest();
        request.setReason("spam");
        request.setUntil(OffsetDateTime.now().minusDays(1));

        assertThatThrownBy(() -> userAdminService.suspend(actorId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Suspension end date must be in the future");

        assertThat(user.getSuspendedUntil()).isNull();
    }

    @Test
    void suspend_nonExistentUser_throwsException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        SuspensionRequest request = new SuspensionRequest();
        request.setReason("spam");
        request.setUntil(OffsetDateTime.now().plusDays(1));

        assertThatThrownBy(() -> userAdminService.suspend(actorId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void suspend_repeated_overwritesPrevious() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);
        user.setSuspendedUntil(OffsetDateTime.now().plusDays(1));
        user.setSuspensionReason("old reason");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        SuspensionRequest request = new SuspensionRequest();
        request.setReason("new reason");
        request.setUntil(OffsetDateTime.now().plusDays(30));

        userAdminService.suspend(actorId, userId, request);

        assertThat(user.getSuspensionReason()).isEqualTo("new reason");

        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).record(eq(actorId), eq("USER_SUSPEND"), eq("USER"), eq(userId), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue()).containsEntry("previousReason", "old reason");
    }

    @Test
    void unsuspend_suspendedUser_clearsFieldsAndAudits() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime until = OffsetDateTime.now().plusDays(3);
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);
        user.setSuspendedUntil(until);
        user.setSuspensionReason("spam");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponse result = userAdminService.unsuspend(actorId, userId);

        assertThat(result).isNotNull();
        assertThat(user.getSuspendedUntil()).isNull();
        assertThat(user.getSuspensionReason()).isNull();

        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).record(eq(actorId), eq("USER_UNSUSPEND"), eq("USER"), eq(userId), detailsCaptor.capture());
        assertThat(detailsCaptor.getValue())
                .containsEntry("previousReason", "spam")
                .containsEntry("previousUntil", until.toString());
    }

    @Test
    void unsuspend_notSuspended_throwsException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userAdminService.unsuspend(actorId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User is not suspended");

        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void unsuspend_expiredSuspension_throwsException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);
        user.setSuspendedUntil(OffsetDateTime.now().minusDays(1));
        user.setSuspensionReason("spam");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userAdminService.unsuspend(actorId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User is not suspended");
    }
}
