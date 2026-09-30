package com.riksa.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.riksa.auth.dto.AuthResponse;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.google.GoogleAuthRequest;
import com.riksa.auth.google.GoogleOAuthService;
import com.riksa.auth.repository.ProvinceRepository;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.security.JwtService;
import com.riksa.auth.token.RefreshTokenRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2ServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ProvinceRepository provinceRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private GoogleOAuthService googleOAuthService;
    @Mock private UserGamificationRepository userGamificationRepository;

    @InjectMocks private OAuth2Service oAuth2Service;

    @Test
    void googleAuth_newUser_createsAccount() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-123");
        payload.setEmail("test@gmail.com");
        payload.setEmailVerified(true);
        payload.set("name", "Test User");
        payload.set("picture", "https://pic.url/avatar.png");

        when(googleOAuthService.verify("id-token")).thenReturn(payload);
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("test")).thenReturn(false);

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setUsername("test");
        savedUser.setRole(Role.USER);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(any(), eq("test"), eq("USER"))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(), eq("test"), eq("USER"))).thenReturn("refresh");

        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("id-token");

        AuthResponse response = oAuth2Service.googleAuth(request);

        assertThat(response).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void googleAuth_unverifiedEmail_doesNotCreateOrLinkAccount() {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-123");
        payload.setEmail("test@gmail.com");
        payload.setEmailVerified(false);
        when(googleOAuthService.verify("id-token")).thenReturn(payload);
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());

        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("id-token");

        assertThatThrownBy(() -> oAuth2Service.googleAuth(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Google account must have a verified email address");
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void googleAuth_existingGoogleUser_logsIn() {
        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setUsername("john");
        existingUser.setGoogleId("google-123");
        existingUser.setRole(Role.USER);

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-123");
        payload.setEmail("john@gmail.com");

        when(googleOAuthService.verify("id-token")).thenReturn(payload);
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existingUser));
        when(jwtService.generateAccessToken(any(), eq("john"), eq("USER"))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(), eq("john"), eq("USER"))).thenReturn("refresh");

        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("id-token");

        AuthResponse response = oAuth2Service.googleAuth(request);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("john");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void googleAuth_suspendedUser_doesNotReceiveTokens() {
        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setUsername("john");
        existingUser.setGoogleId("google-123");
        existingUser.setRole(Role.USER);
        existingUser.setSuspendedUntil(java.time.OffsetDateTime.now().plusDays(1));

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-123");
        when(googleOAuthService.verify("id-token")).thenReturn(payload);
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existingUser));

        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("id-token");

        assertThatThrownBy(() -> oAuth2Service.googleAuth(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Account suspended until");
        verify(jwtService, never()).generateAccessToken(any(), any(), any());
    }
}
