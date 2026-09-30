package com.riksa.auth.service;

import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.otp.OtpService;
import com.riksa.auth.repository.ProvinceRepository;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.security.JwtService;
import com.riksa.auth.telegram.TelegramAuthRequest;
import com.riksa.auth.token.EmailVerificationTokenRepository;
import com.riksa.auth.token.RefreshTokenRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
class TelegramAuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ProvinceRepository provinceRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private OtpService otpService;
    @Mock private UserGamificationRepository userGamificationRepository;

    @InjectMocks private AuthService authService;

    @Test
    void telegramAuth_existingUser_returnsSession() {
        User user = telegramUser();
        when(otpService.verifyOtp("123456789", "123456")).thenReturn(true);
        when(userRepository.findByTelegramId("123456789")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(jwtService.generateAccessToken(user.getId(), "sophea", "USER")).thenReturn("access");
        when(jwtService.generateRefreshToken(user.getId(), "sophea", "USER")).thenReturn("refresh");

        var response = authService.telegramAuth(request(null, null));

        assertThat(response.getUsername()).isEqualTo("sophea");
        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(user.getPhoneVerified()).isTrue();
    }

    @Test
    void telegramAuth_newUser_createsAccountAndSession() {
        when(otpService.verifyOtp("123456789", "123456")).thenReturn(true);
        when(userRepository.findByTelegramId("123456789")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("sophea")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateAccessToken(any(UUID.class), eq("sophea"), eq("USER"))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(UUID.class), eq("sophea"), eq("USER"))).thenReturn("refresh");

        var response = authService.telegramAuth(request("sophea", "Sok Sophea"));

        assertThat(response.getUsername()).isEqualTo("sophea");
        verify(userGamificationRepository).save(any());
    }

    @Test
    void telegramAuth_invalidOtp_doesNotLookUpOrCreateUser() {
        when(otpService.verifyOtp("123456789", "123456")).thenReturn(false);

        assertThatThrownBy(() -> authService.telegramAuth(request(null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid or expired OTP code");
        verify(userRepository, never()).findByTelegramId(any());
        verify(userRepository, never()).save(any());
    }

    private TelegramAuthRequest request(String username, String displayName) {
        TelegramAuthRequest request = new TelegramAuthRequest();
        request.setIdentifier("123456789");
        request.setCode("123456");
        request.setUsername(username);
        request.setDisplayName(displayName);
        return request;
    }

    private User telegramUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setTelegramId("123456789");
        user.setUsername("sophea");
        user.setDisplayName("Sok Sophea");
        user.setRole(Role.USER);
        user.setPhoneVerified(false);
        return user;
    }
}
