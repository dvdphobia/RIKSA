package com.riksa.auth.service;

import com.riksa.auth.dto.*;
import com.riksa.auth.entity.Role;
import com.riksa.auth.event.EmailVerificationRequestedEvent;
import com.riksa.auth.entity.User;
import com.riksa.auth.otp.OtpService;
import com.riksa.auth.repository.ProvinceRepository;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.security.JwtService;
import com.riksa.auth.token.EmailVerificationTokenRepository;
import com.riksa.auth.token.RefreshTokenRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.common.event.EventPublisher;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
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
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private OtpService otpService;
    @Mock private EventPublisher eventPublisher;
    @Mock private UserGamificationRepository userGamificationRepository;

    @InjectMocks private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockoutMinutes", 15);
    }

    @Test
    void register_duplicateUsername_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john");
        request.setDisplayName("John");
        request.setEmail("john@example.com");
        request.setPassword("Password1");

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
        request.setPassword("Password1");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is already taken");
    }

    @Test
    void login_wrongPassword_throwsException() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("john");
        user.setPassword("encoded-password");
        user.setFailedLoginAttempts(0);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "encoded-password")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("wrongpass");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username or password");

        verify(userRepository).recordFailedLogin(eq(user.getId()), eq(5), any(OffsetDateTime.class));
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
    void login_lockedAccount_throwsException() {
        User user = new User();
        user.setUsername("john");
        user.setPassword("encoded-password");
        user.setFailedLoginAttempts(5);
        user.setLockedUntil(OffsetDateTime.now().plusMinutes(15));

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("Password1");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account is temporarily locked. Try again later.");
    }

    @Test
    void login_suspendedAccount_throwsAccessDenied() {
        User user = new User();
        user.setUsername("john");
        user.setPassword("encoded-password");
        user.setFailedLoginAttempts(0);
        OffsetDateTime until = OffsetDateTime.now().plusDays(7);
        user.setSuspendedUntil(until);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("Password1");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Account suspended until");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void login_expiredSuspension_succeeds() {
        User user = new User();
        user.setUsername("john");
        user.setPassword("encoded-password");
        user.setFailedLoginAttempts(0);
        user.setSuspendedUntil(OffsetDateTime.now().minusDays(1));
        user.setSuspensionReason("old spam");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1", "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), eq("john"), eq("USER"))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(), eq("john"), eq("USER"))).thenReturn("refresh");

        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("Password1");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
    }

    @Test
    void refreshToken_suspendedUser_throwsAccessDenied() {
        String token = "some.jwt.token";
        User user = new User();
        user.setSuspendedUntil(OffsetDateTime.now().plusDays(7));

        com.riksa.auth.token.RefreshToken stored = new com.riksa.auth.token.RefreshToken();
        stored.setUser(user);
        stored.setExpiresAt(OffsetDateTime.now().plusDays(1));

        when(jwtService.isRefreshTokenValid(token)).thenReturn(true);
        when(refreshTokenRepository.findByTokenAndRevokedFalse(token)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refreshToken(token))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Account suspended until");

        verify(jwtService, never()).generateAccessToken(any(), any(), any());
    }

    @Test
    void refreshToken_revokedToken_throwsException() {
        String token = "some.jwt.token";

        when(jwtService.isRefreshTokenValid(token)).thenReturn(true);
        when(refreshTokenRepository.findByTokenAndRevokedFalse(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token not found or revoked");
    }

    @Test
    void refreshToken_rejectsAccessToken() {
        String token = "access.jwt.token";
        when(jwtService.isRefreshTokenValid(token)).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid or expired refresh token");

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void register_success_returnsAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setDisplayName("New User");
        request.setEmail("new@example.com");
        request.setPassword("Password1");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");
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
        verify(emailVerificationTokenRepository).save(any());
        verify(eventPublisher).publish(any(EmailVerificationRequestedEvent.class));
    }

    @Test
    void resendEmailVerification_unverifiedAccountPublishesEmailRequest() {
        User user = new User();
        user.setEmail("new@example.com");
        user.setEmailVerified(false);
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(user));

        authService.resendEmailVerification("new@example.com");

        verify(emailVerificationTokenRepository).save(any());
        verify(eventPublisher).publish(any(EmailVerificationRequestedEvent.class));
    }

    @Test
    void resendEmailVerification_unknownAddressDoesNothing() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        authService.resendEmailVerification("unknown@example.com");

        verifyNoInteractions(emailVerificationTokenRepository, eventPublisher);
    }

    @Test
    void login_success_resetsFailedAttempts() {
        User user = new User();
        user.setUsername("john");
        user.setPassword("encoded-password");
        user.setFailedLoginAttempts(2);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1", "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), eq("john"), eq("USER"))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(), eq("john"), eq("USER"))).thenReturn("refresh");

        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("Password1");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void verifyOtp_telegram_setsVerified() {
        User user = new User();
        user.setTelegramId("123456789");
        user.setPhoneVerified(false);

        when(otpService.verifyOtp("123456789", "123456")).thenReturn(true);
        when(userRepository.findByTelegramId("123456789")).thenReturn(Optional.of(user));

        boolean result = authService.verifyOtp("123456789", "123456");

        assertThat(result).isTrue();
        assertThat(user.getPhoneVerified()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void verifyOtp_invalidCode_returnsFalse() {
        when(otpService.verifyOtp("123456789", "wrong")).thenReturn(false);

        boolean result = authService.verifyOtp("123456789", "wrong");

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void forgotPassword_existingUser_sendsOtp() {
        User user = new User();
        user.setUsername("john");
        user.setTelegramId("123456789");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        authService.forgotPassword("john");

        verify(otpService).generateOtp("123456789");
    }

    @Test
    void forgotPassword_nonExistentUser_throwsException() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nobody")).thenReturn(Optional.empty());
        when(userRepository.findByTelegramId("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.forgotPassword("nobody"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No account found for this identifier");
    }

    @Test
    void resetPassword_validOtp_revokesExistingRefreshTokens() {
        User user = new User();
        user.setUsername("john");
        user.setTelegramId("123456789");
        user.setPassword("old-password");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(otpService.verifyOtp("123456789", "123456")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword1")).thenReturn("new-encoded-password");

        authService.resetPassword("john", "123456", "NewPassword1");

        assertThat(user.getPassword()).isEqualTo("new-encoded-password");
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUser(user);
    }

    @Test
    void verifyEmail_validToken_verifies() {
        com.riksa.auth.token.EmailVerificationToken token = new com.riksa.auth.token.EmailVerificationToken();
        token.setToken("abc123");
        token.setExpiresAt(OffsetDateTime.now().plusHours(1));
        token.setVerified(false);

        User user = new User();
        user.setEmail("test@example.com");
        user.setEmailVerified(false);
        token.setUser(user);

        when(emailVerificationTokenRepository.findByTokenAndVerifiedFalse("abc123")).thenReturn(Optional.of(token));

        boolean result = authService.verifyEmail("abc123");

        assertThat(result).isTrue();
        assertThat(user.getEmailVerified()).isTrue();
        verify(emailVerificationTokenRepository).save(token);
        verify(userRepository).save(user);
    }

    @Test
    void getProfile_existingUser_returnsProfile() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("john");
        user.setDisplayName("John");
        user.setEmail("john@example.com");
        user.setEmailVerified(true);
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResponse profile = authService.getProfile(userId);

        assertThat(profile).isNotNull();
        assertThat(profile.getUsername()).isEqualTo("john");
        assertThat(profile.getEmailVerified()).isTrue();
    }

    @Test
    void updateProfile_withBundledAvatar_updatesAvatar() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("john");
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse profile = authService.updateProfile(
                userId, null, null, null, null, "/avatars/avatar-17.png", null);

        assertThat(user.getAvatarUrl()).isEqualTo("/avatars/avatar-17.png");
        assertThat(profile.getAvatarUrl()).isEqualTo("/avatars/avatar-17.png");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_withNonBundledAvatar_rejectsRequest() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> authService.updateProfile(
                userId, null, null, null, null, "https://example.com/avatar.png", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("avatarUrl must be one of the bundled preset avatars");

        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).findById(any(UUID.class));
    }

    @Test
    void updateProfileRequest_avatarUrlAcceptsOnlyBundledAvatarPaths() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            UpdateProfileRequest validRequest = new UpdateProfileRequest();
            validRequest.setAvatarUrl("/avatars/avatar-30.png");
            UpdateProfileRequest invalidRequest = new UpdateProfileRequest();
            invalidRequest.setAvatarUrl("/avatars/avatar-31.png");

            assertThat(validator.validate(validRequest)).isEmpty();
            assertThat(validator.validate(invalidRequest))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactly("avatarUrl");
        }
    }

    @Test
    void login_maxFailedAttempts_locksAccount() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("john");
        user.setPassword("encoded-password");
        user.setFailedLoginAttempts(4);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "encoded-password")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("wrongpass");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository).recordFailedLogin(eq(user.getId()), eq(5), any(OffsetDateTime.class));
    }
}
