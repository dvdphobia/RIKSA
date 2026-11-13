package com.riksa.auth.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "competition-test-secret-that-is-at-least-256-bits-long",
            900_000,
            2_592_000_000L);

    @Test
    void generatedRefreshTokensAreUnique() {
        UUID userId = UUID.randomUUID();

        String first = jwtService.generateRefreshToken(userId, "learner", "USER");
        String second = jwtService.generateRefreshToken(userId, "learner", "USER");

        assertThat(first).isNotEqualTo(second);
        assertThat(jwtService.getUserId(first)).isEqualTo(userId);
        assertThat(jwtService.getUserId(second)).isEqualTo(userId);
    }

    @Test
    void accessAndRefreshTokensAreOnlyValidForTheirOwnPurpose() {
        UUID userId = UUID.randomUUID();
        String accessToken = jwtService.generateAccessToken(userId, "learner", "USER");
        String refreshToken = jwtService.generateRefreshToken(userId, "learner", "USER");

        assertThat(jwtService.isAccessTokenValid(accessToken)).isTrue();
        assertThat(jwtService.isRefreshTokenValid(accessToken)).isFalse();
        assertThat(jwtService.isAccessTokenValid(refreshToken)).isFalse();
        assertThat(jwtService.isRefreshTokenValid(refreshToken)).isTrue();
    }
}
