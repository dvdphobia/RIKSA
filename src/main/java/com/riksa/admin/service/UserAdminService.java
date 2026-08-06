package com.riksa.admin.service;

import com.riksa.admin.dto.SuspensionRequest;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<UserProfileResponse> list(int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        var pageable = org.springframework.data.domain.PageRequest.of(page, safeSize);
        return userRepository.findAll(pageable).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserProfileResponse updateRole(UUID actorId, UUID userId, String roleStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Role newRole;
        try {
            newRole = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleStr + ". Valid roles: USER, MODERATOR, ADMIN");
        }

        // Only allow promoting to MODERATOR or ADMIN — can't demote below the actor
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
    public UserProfileResponse suspend(UUID actorId, UUID userId, SuspensionRequest request) {
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
    public UserProfileResponse unsuspend(UUID actorId, UUID userId) {
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

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getEmailVerified(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getGender(),
                user.getPhoneNumber(),
                user.getPhoneVerified(),
                user.getRole().name(),
                user.getCountryId(),
                user.getCreatedAt()
        );
    }
}