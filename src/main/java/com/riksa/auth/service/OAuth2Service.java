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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class OAuth2Service {

    private final UserRepository userRepository;
    private final ProvinceRepository provinceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleOAuthService googleOAuthService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private static final String[] AVAILABLE_AVATARS = {
            "avatar-1.png", "avatar-2.png", "avatar-3.png", "avatar-4.png", "avatar-5.png",
            "avatar-6.png", "avatar-7.png", "avatar-8.png", "avatar-9.png", "avatar-10.png"
    };

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
