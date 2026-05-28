package com.riksa.auth;

import com.riksa.auth.dto.*;
import com.riksa.auth.entity.UserSession;
import com.riksa.auth.repository.UserSessionRepository;
import com.riksa.common.entity.User;
import com.riksa.common.enums.UserRole;
import com.riksa.common.exception.AppException;
import com.riksa.common.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final OtpService otpService;
    private final JwtService jwtService;

    @Value("${app.google.client-id}")
    private String googleClientId;

    @Transactional
    public AuthResponse verifyOtpAndAuth(VerifyOtpRequest request) {
        String normalizedPhone = PhoneUtils.normalize(request.getPhoneNumber());

        otpService.verifyOtp(request.getPhoneNumber(), request.getCode());

        Optional<User> existingUser = userRepository.findByPhoneNumber(normalizedPhone);

        User user;
        boolean isNewUser;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            isNewUser = false;
        } else {
            user = createPhoneUser(normalizedPhone);
            isNewUser = true;
        }

        user.setIsPhoneVerified(true);
        userRepository.save(user);

        return buildAuthResponse(user, isNewUser);
    }

    @Transactional
    public AuthResponse authenticateWithGoogle(GoogleAuthRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleToken(request.getIdToken());

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String avatarUrl = (String) payload.get("picture");

        Optional<User> existingUser = userRepository.findByGoogleId(googleId);

        User user;
        boolean isNewUser;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            isNewUser = false;
        } else {
            Optional<User> emailUser = email != null ? userRepository.findByEmail(email) : Optional.empty();
            if (emailUser.isPresent()) {
                user = emailUser.get();
                user.setGoogleId(googleId);
                if (avatarUrl != null) user.setAvatarUrl(avatarUrl);
                userRepository.save(user);
                isNewUser = false;
            } else {
                user = createGoogleUser(googleId, email, name, avatarUrl);
                isNewUser = true;
            }
        }

        return buildAuthResponse(user, isNewUser);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        UserSession session = sessionRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            sessionRepository.delete(session);
            throw new AppException("Refresh token has expired. Please log in again", HttpStatus.UNAUTHORIZED);
        }

        User user = session.getUser();

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        session.setRefreshToken(refreshToken);
        session.setExpiresAt(OffsetDateTime.now().plusMillis(jwtService.getRefreshTokenExpiration()));
        sessionRepository.save(session);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getRefreshTokenExpiration() / 1000)
                .user(mapToUserResponse(user))
                .isNewUser(false)
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        sessionRepository.deleteByRefreshToken(refreshToken);
    }

    private User createPhoneUser(String phoneNumber) {
        User user = User.builder()
                .phoneNumber(phoneNumber)
                .role(UserRole.STUDENT)
                .trustLevel(0)
                .isBanned(false)
                .isPhoneVerified(true)
                .isEmailVerified(false)
                .build();
        return userRepository.save(user);
    }

    private User createGoogleUser(String googleId, String email, String name, String avatarUrl) {
        User user = User.builder()
                .googleId(googleId)
                .email(email)
                .name(name)
                .avatarUrl(avatarUrl)
                .role(UserRole.STUDENT)
                .trustLevel(0)
                .isBanned(false)
                .isPhoneVerified(false)
                .isEmailVerified(true)
                .build();
        return userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user, boolean isNewUser) {
        if (user.getIsBanned()) {
            throw new AppException("Your account has been suspended", HttpStatus.FORBIDDEN);
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        UserSession session = UserSession.builder()
                .user(user)
                .refreshToken(refreshToken)
                .expiresAt(OffsetDateTime.now().plusMillis(jwtService.getRefreshTokenExpiration()))
                .build();
        sessionRepository.save(session);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getRefreshTokenExpiration() / 1000)
                .user(mapToUserResponse(user))
                .isNewUser(isNewUser)
                .build();
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new AppException("Invalid Google ID token", HttpStatus.UNAUTHORIZED);
            }
            return token.getPayload();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException("Failed to verify Google token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .nameKhmer(user.getNameKhmer())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .schoolId(user.getSchool() != null ? user.getSchool().getId() : null)
                .provinceId(user.getProvince() != null ? user.getProvince().getId() : null)
                .role(user.getRole())
                .grade(user.getGrade())
                .isPhoneVerified(user.getIsPhoneVerified())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
