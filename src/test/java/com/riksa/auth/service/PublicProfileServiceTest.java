package com.riksa.auth.service;

import com.riksa.auth.dto.PublicProfileResponse;
import com.riksa.auth.entity.Province;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.gamification.dto.GamificationProfileResponse;
import com.riksa.gamification.service.GamificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicProfileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private GamificationService gamificationService;

    @InjectMocks private PublicProfileService publicProfileService;

    @Test
    void getProfile_existingUser_returnsPublicIdentityAndGamification() {
        UUID userId = UUID.randomUUID();
        UUID provinceId = UUID.randomUUID();
        OffsetDateTime joinedAt = OffsetDateTime.parse("2026-08-01T10:15:30+07:00");

        Province province = new Province();
        province.setId(provinceId);
        province.setNameKhmer("ភ្នំពេញ");
        province.setNameLatin("Phnom Penh");

        User user = new User();
        user.setId(userId);
        user.setUsername("sophea");
        user.setDisplayName("Sok Sophea");
        user.setAvatarUrl("/avatars/avatar-17.png");
        user.setBio("Learning STEM & Tech");
        user.setRole(Role.USER);
        user.setCountryId("KH");
        user.setProvince(province);
        user.setCreatedAt(joinedAt);
        user.setEmail("private@example.com");
        user.setPhoneNumber("+85512345678");

        GamificationProfileResponse gamification = new GamificationProfileResponse();
        gamification.setUserId(userId);
        gamification.setTotalXp(5_550);
        gamification.setCurrentStreak(13);
        gamification.setHighestStreak(21);
        gamification.setCurrentTier("LEGEND");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(gamificationService.getProfile(userId)).thenReturn(gamification);

        PublicProfileResponse response = publicProfileService.getProfile(userId);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.username()).isEqualTo("sophea");
        assertThat(response.displayName()).isEqualTo("Sok Sophea");
        assertThat(response.province().id()).isEqualTo(provinceId);
        assertThat(response.province().nameKhmer()).isEqualTo("ភ្នំពេញ");
        assertThat(response.joinedAt()).isEqualTo(joinedAt);
        assertThat(response.gamification()).isSameAs(gamification);
    }

    @Test
    void getProfile_userWithoutProvince_returnsNullProvince() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("sophea");
        user.setDisplayName("Sok Sophea");
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(gamificationService.getProfile(userId)).thenReturn(new GamificationProfileResponse());

        PublicProfileResponse response = publicProfileService.getProfile(userId);

        assertThat(response.province()).isNull();
    }

    @Test
    void getProfile_unknownUser_throwsNotFoundWithoutLoadingGamification() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicProfileService.getProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: " + userId);

        verify(gamificationService, never()).getProfile(userId);
    }
}
