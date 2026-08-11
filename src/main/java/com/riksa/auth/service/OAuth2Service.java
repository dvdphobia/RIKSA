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
public class OAuth2Service {

    private final UserRepository userRepository;
    private final ProvinceRepository provinceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleOAuthService googleOAuthService;
    private final UserGamificationRepository userGamificationRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private static final String[] AVAILABLE_AVATARS = {
            "/avatars/avatar-1.png", "/avatars/avatar-2.png", "/avatars/avatar-3.png", "/avatars/avatar-4.png", "/avatars/avatar-5.png",
            "/avatars/avatar-6.png", "/avatars/avatar-7.png", "/avatars/avatar-8.png", "/avatars/avatar-9.png", "/avatars/avatar-10.png",
             "/avatars/avatar-11.png", "/avatars/avatar-12.png", "/avatars/avatar-13.png", "/avatars/avatar-14.png", "/avatars/avatar-15.png",
            "/avatars/avatar-16.png", "/avatars/avatar-17.png", "/avatars/avatar-18.png", "/avatars/avatar-19.png", "/avatars/avatar-20.png",
            "/avatars/avatar-21.png", "/avatars/avatar-22.png", "/avatars/avatar-23.png", "/avatars/avatar-24.png", "/avatars/avatar-25.png",
            "/avatars/avatar-26.png", "/avatars/avatar-27.png", "/avatars/avatar-28.png", "/avatars/avatar-29.png", "/avatars/avatar-30.png"
    };

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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

            // Create gamification row for new Google OAuth users
            if (!userGamificationRepository.existsByUserId(user.getId())) {
                UserGamification gamification = new UserGamification();
                gamification.setUserId(user.getId());
                userGamificationRepository.save(gamification);
            }
        }

        return buildAuthResponse(user);
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
        return AVAILABLE_AVATARS[SECURE_RANDOM.nextInt(AVAILABLE_AVATARS.length)];
    }
}
