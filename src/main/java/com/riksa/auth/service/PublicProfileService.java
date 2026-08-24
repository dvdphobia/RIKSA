package com.riksa.auth.service;

import com.riksa.auth.dto.ProvinceResponse;
import com.riksa.auth.dto.PublicProfileResponse;
import com.riksa.auth.entity.Province;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.gamification.dto.GamificationProfileResponse;
import com.riksa.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicProfileService {

    private final UserRepository userRepository;
    private final GamificationService gamificationService;

    @Transactional(readOnly = true)
    public PublicProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        GamificationProfileResponse gamification = gamificationService.getProfile(userId);

        return new PublicProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getRole().name(),
                user.getCountryId(),
                mapProvince(user.getProvince()),
                user.getCreatedAt(),
                gamification
        );
    }

    private ProvinceResponse mapProvince(Province province) {
        if (province == null) {
            return null;
        }
        return new ProvinceResponse(
                province.getId(),
                province.getNameKhmer(),
                province.getNameLatin()
        );
    }
}
