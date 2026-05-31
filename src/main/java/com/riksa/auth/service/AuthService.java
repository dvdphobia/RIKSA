package com.riksa.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.riksa.auth.dto.*;
import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import com.riksa.auth.google.GoogleAuthRequest;
import com.riksa.auth.google.GoogleOAuthService;
import com.riksa.auth.otp.OtpChannel;
import com.riksa.auth.otp.OtpService;
import com.riksa.auth.token.RefreshToken;
import com.riksa.auth.token.RefreshTokenRepository;
import com.riksa.auth.repository.ProvinceRepository;
import com.riksa.auth.repository.UserRepository;
import com.riksa.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProvinceRepository provinceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleOAuthService googleOAuthService;
    private final OtpService otpService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

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
        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

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

    @Transactional
    public AuthResponse googleAuth(GoogleAuthRequest request) {
        GoogleIdToken.Payload payload = googleOAuthService.verify(request.getIdToken());
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        User user = userRepository.findByGoogleId(googleId).orElse(null);

        if (user == null && email != null) {
            user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                user.setGoogleId(googleId);
                if (picture != null) {
                    user.setAvatarUrl(picture);
                }
                user = userRepository.save(user);
            }
        }

        if (user == null) {
            user = new User();
            user.setGoogleId(googleId);
            user.setEmail(email);
            user.setRole(Role.USER);
            user.setAvatarUrl(picture != null ? picture : generateRandomAvatar());
            user.setDisplayName(name != null ? name : "User" + UUID.randomUUID().toString().substring(0, 8));

            String username;
            if (request.getUsername() != null && !request.getUsername().isBlank()
                    && !userRepository.existsByUsername(request.getUsername())) {
                username = request.getUsername();
            } else {
                String baseUsername = email != null ? email.split("@")[0] : "user";
                username = baseUsername;
                int suffix = 1;
                while (userRepository.existsByUsername(username)) {
                    username = baseUsername + suffix++;
                }
            }
            user.setUsername(username);

            if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
                user.setDisplayName(request.getDisplayName());
            }
            if (request.getProvinceId() != null) {
                provinceRepository.findById(request.getProvinceId())
                        .ifPresent(user::setProvince);
            }

            user = userRepository.save(user);
        }

        return buildAuthResponse(user);
    }

    public void sendOtp(String identifier, OtpChannel channel) {
        otpService.generateOtp(identifier, channel);
    }

    @Transactional
    public boolean verifyPhoneOtp(String identifier, OtpChannel channel, String code) {
        boolean verified = otpService.verifyOtp(identifier, channel, code);
        if (verified && channel == OtpChannel.SMS) {
            userRepository.findByPhoneNumber(identifier).ifPresent(user -> {
                user.setPhoneVerified(true);
                userRepository.save(user);
            });
        }
        return verified;
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

    private String generateRandomAvatar() {
        Random random = new Random();
        return AVAILABLE_AVATARS[random.nextInt(AVAILABLE_AVATARS.length)];
    }
}
