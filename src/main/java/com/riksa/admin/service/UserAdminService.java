package com.riksa.admin.service;

import com.riksa.admin.dto.AdminUserResponse;
import com.riksa.admin.dto.SuspensionRequest;
import com.riksa.common.audit.AuditLogService;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> list(String query, Role role, Boolean suspended, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        String safeQuery = (query == null || query.isBlank()) ? "" : query.trim();
        return userRepository.search(safeQuery, role, suspended, OffsetDateTime.now(), PageRequest.of(page, safeSize))
                .map(this::toResponse);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.USER_SECURITY_STATE, key = "#userId.toString()")
    public AdminUserResponse updateRole(UUID actorId, UUID userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (newRole == Role.USER) {
            throw new IllegalArgumentException("Cannot set role to USER via admin endpoint");
        }

        Role oldRole = user.getRole();
        user.setRole(newRole);
        user = userRepository.save(user);

        auditLogService.record(actorId, "USER_ROLE_CHANGE", "USER", userId,
                Map.of("oldRole", oldRole.name(), "newRole", newRole.name()));

        return toResponse(user);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.USER_SECURITY_STATE, key = "#userId.toString()")
    public AdminUserResponse suspend(UUID actorId, UUID userId, SuspensionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getRole() == Role.MODERATOR || user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot suspend staff accounts");
        }
        if (request.getUntil().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Suspension end date must be in the future");
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", request.getReason());
        details.put("until", request.getUntil().toString());
        if (user.getSuspensionReason() != null) {
            details.put("previousReason", user.getSuspensionReason());
        }

        user.setSuspendedUntil(request.getUntil());
        user.setSuspensionReason(request.getReason());
        user = userRepository.save(user);

        auditLogService.record(actorId, "USER_SUSPEND", "USER", userId, details);

        return toResponse(user);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.USER_SECURITY_STATE, key = "#userId.toString()")
    public AdminUserResponse unsuspend(UUID actorId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!user.isSuspended()) {
            throw new IllegalArgumentException("User is not suspended");
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("previousReason", user.getSuspensionReason());
        details.put("previousUntil", user.getSuspendedUntil().toString());

        user.setSuspendedUntil(null);
        user.setSuspensionReason(null);
        user = userRepository.save(user);

        auditLogService.record(actorId, "USER_UNSUSPEND", "USER", userId, details);

        return toResponse(user);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getEmailVerified(),
                user.getAvatarUrl(),
                user.getRole().name(),
                user.getSuspendedUntil(),
                user.getSuspensionReason(),
                user.getLockedUntil(),
                user.getCreatedAt()
        );
    }
}
