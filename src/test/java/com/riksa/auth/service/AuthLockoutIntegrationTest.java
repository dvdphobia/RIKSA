package com.riksa.auth.service;

import com.riksa.auth.dto.LoginRequest;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "login.max-failed-attempts=2")
@EnabledIfEnvironmentVariable(named = "RIKSA_RUN_INTEGRATION_TESTS", matches = "true")
class AuthLockoutIntegrationTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void failedAttemptsSurviveLoginRollbackAndLockTheAccount() {
        User user = new User();
        user.setUsername("lockout-" + UUID.randomUUID());
        user.setDisplayName("Lockout Test");
        user.setEmail(UUID.randomUUID() + "@example.test");
        user.setPassword(passwordEncoder.encode("CorrectPassword1"));
        User saved = userRepository.saveAndFlush(user);

        try {
            LoginRequest request = new LoginRequest();
            request.setUsername(saved.getUsername());
            request.setPassword("WrongPassword1");

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(IllegalArgumentException.class);
            User afterFirstFailure = userRepository.findById(saved.getId()).orElseThrow();
            assertThat(afterFirstFailure.getFailedLoginAttempts()).isEqualTo(1);
            assertThat(afterFirstFailure.isLocked()).isFalse();

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(IllegalArgumentException.class);
            User afterSecondFailure = userRepository.findById(saved.getId()).orElseThrow();
            assertThat(afterSecondFailure.getFailedLoginAttempts()).isEqualTo(2);
            assertThat(afterSecondFailure.isLocked()).isTrue();
        } finally {
            userRepository.deleteById(saved.getId());
        }
    }
}
