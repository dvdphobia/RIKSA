package com.riksa.auth.service;

import com.riksa.auth.dto.*;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.otp.OtpChannel;
import com.riksa.auth.otp.OtpService;
import com.riksa.auth.repository.ProvinceRepository;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.security.JwtService;
import com.riksa.auth.token.RefreshTokenRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ProvinceRepository provinceRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private OtpService otpService;

    @InjectMocks private AuthService authService;

    @Test
    void register_duplicateUsername_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john");
        request.setDisplayName("John");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is already taken");
    }

    @Test
    void register_duplicateEmail_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setDisplayName("New User");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is already taken");
    }

    @Test
    void login_wrongPassword_throwsException() {
        User user = new User();
        user.setUsername("john");
        user.setPassword("encoded-password");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "encoded-password")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("wrongpass");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void login_usernameNotFound_throwsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUsername("ghost");
        request.setPassword("any");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void refreshToken_revokedToken_throwsException() {
        String token = "some.jwt.token";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(refreshTokenRepository.findByTokenAndRevokedFalse(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token not found or revoked");
    }

    @Test
    void register_success_returnsAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setDisplayName("New User");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(jwtService.generateAccessToken(any(UUID.class), eq("newuser"), eq("USER"))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(UUID.class), eq("newuser"), eq("USER"))).thenReturn("refresh-token");

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setUsername("newuser");
        savedUser.setRole(Role.USER);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getRole()).isEqualTo("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void verifyPhoneOtp_smsChannel_setsPhoneVerified() {
        User user = new User();
        user.setPhoneNumber("+85512345678");
        user.setPhoneVerified(false);

        when(otpService.verifyOtp("+85512345678", OtpChannel.SMS, "123456")).thenReturn(true);
        when(userRepository.findByPhoneNumber("+85512345678")).thenReturn(Optional.of(user));

        boolean result = authService.verifyPhoneOtp("+85512345678", OtpChannel.SMS, "123456");

        assertThat(result).isTrue();
        assertThat(user.getPhoneVerified()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void verifyPhoneOtp_telegramChannel_doesNotSetPhoneVerified() {
        when(otpService.verifyOtp("123456789", OtpChannel.TELEGRAM, "123456")).thenReturn(true);

        boolean result = authService.verifyPhoneOtp("123456789", OtpChannel.TELEGRAM, "123456");

        assertThat(result).isTrue();
        verify(userRepository, never()).save(any(User.class));
    }
}
