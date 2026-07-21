package com.riksa.auth.service;

import com.riksa.auth.dto.*;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.otp.OtpService;
import com.riksa.auth.repository.ProvinceRepository;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.security.JwtService;
import com.riksa.auth.token.EmailVerificationToken;
import com.riksa.auth.token.EmailVerificationTokenRepository;
import com.riksa.auth.token.RefreshToken;
import com.riksa.auth.token.RefreshTokenRepository;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.repository.UserGamificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserGamificationRepository userGamificationRepository;

    private final UserRepository userRepository;
    private final ProvinceRepository provinceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${login.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${login.lockout-minutes:15}")
    private int lockoutMinutes;

    private static final String[] AVAILABLE_AVATARS = {
            "avatar-1.png", "avatar-2.png", "avatar-3.png", "avatar-4.png", "avatar-5.png",
            "avatar-6.png", "avatar-7.png", "avatar-8.png", "avatar-9.png", "avatar-10.png"
    };

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setDisplayName(request.getDisplayName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAvatarUrl(generateRandomAvatar());
        user.setGender(request.getGender());

        if (request.getProvinceId() != null) {
            provinceRepository.findById(request.getProvinceId())
                    .ifPresent(user::setProvince);
        }

        User saved = userRepository.save(user);

        // Create gamification row so the XP hot path doesn't crash on first lesson completion
        UserGamification gamification = new UserGamification();
        gamification.setUserId(saved.getId());
        userGamificationRepository.save(gamification);

        sendEmailVerification(saved);
        return buildAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (user.isLocked()) {
            throw new IllegalArgumentException("Account is temporarily locked. Try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user);
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        RefreshToken stored = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found or revoked"));

        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Refresh token has expired");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String token) {
        refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapToProfile(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, String displayName, String bio, String gender, UUID provinceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (displayName != null) user.setDisplayName(displayName);
        if (bio != null) user.setBio(bio);
        if (gender != null) user.setGender(gender);
        if (provinceId != null) {
            provinceRepository.findById(provinceId).ifPresent(user::setProvince);
        }

        return mapToProfile(userRepository.save(user));
    }

    @Transactional
    public void forgotPassword(String identifier) {
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .or(() -> userRepository.findByTelegramId(identifier))
                .orElseThrow(() -> new IllegalArgumentException("No account found for this identifier"));

        if (user.getTelegramId() != null) {
            otpService.generateOtp(user.getTelegramId());
        } else {
            throw new IllegalArgumentException("Password reset requires a verified Telegram account. Please contact support.");
        }
    }

    @Transactional
    public void resetPassword(String identifier, String code, String newPassword) {
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .or(() -> userRepository.findByTelegramId(identifier))
                .orElseThrow(() -> new IllegalArgumentException("No account found for this identifier"));

        boolean verified = otpService.verifyOtp(user.getTelegramId(), code);
        if (!verified) {
            throw new IllegalArgumentException("Invalid or expired OTP code");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public boolean verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findByTokenAndVerifiedFalse(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification token"));

        if (verificationToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Verification token has expired");
        }

        verificationToken.setVerified(true);
        emailVerificationTokenRepository.save(verificationToken);

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        return true;
    }

    @Transactional
    public void sendOtp(String telegramChatId) {
        otpService.generateOtp(telegramChatId);
    }

    @Transactional
    public boolean verifyOtp(String telegramChatId, String code) {
        boolean verified = otpService.verifyOtp(telegramChatId, code);
        if (verified) {
            userRepository.findByTelegramId(telegramChatId).ifPresent(user -> {
                user.setPhoneVerified(true);
                userRepository.save(user);
            });
        }
        return verified;
    }

    // --- Private helpers ---

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(OffsetDateTime.now().plusMinutes(lockoutMinutes));
            log.warn("Account locked for user {} after {} failed attempts", user.getUsername(), attempts);
        }

        userRepository.save(user);
    }

    private void sendEmailVerification(User user) {
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(OffsetDateTime.now().plusHours(24));

        emailVerificationTokenRepository.save(verificationToken);

        // Stub: In production, send email with verification link
        log.info("Email verification token for {}: /api/auth/verify-email?token={}", user.getEmail(), token);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String refreshTokenJwt = jwtService.generateRefreshToken(user.getId(), user.getUsername(), user.getRole().name());

        RefreshToken rt = new RefreshToken();
        rt.setToken(refreshTokenJwt);
        rt.setUser(user);
        rt.setExpiresAt(OffsetDateTime.now().plus(Duration.ofMillis(refreshExpiration)));
        refreshTokenRepository.save(rt);

        return new AuthResponse(user.getId(), accessToken, refreshTokenJwt, user.getUsername(), user.getRole().name());
    }

    private UserProfileResponse mapToProfile(User user) {
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

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateRandomAvatar() {
        return AVAILABLE_AVATARS[SECURE_RANDOM.nextInt(AVAILABLE_AVATARS.length)];
    }
}
