package com.riksa.admin.service;

import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;

    @Transactional
    public UserProfileResponse updateRole(UUID userId, String roleStr) {
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

        user.setRole(newRole);
        user = userRepository.save(user);

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