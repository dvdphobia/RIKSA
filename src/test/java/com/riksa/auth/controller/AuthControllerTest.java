package com.riksa.auth.controller;

import com.riksa.auth.security.JwtAuthenticationFilter;
import com.riksa.auth.security.JwtService;
import com.riksa.auth.security.RateLimitFilter;
import com.riksa.auth.security.RedisRateLimiter;
import com.riksa.auth.security.SecurityConfig;
import com.riksa.auth.security.UserSecurityState;
import com.riksa.auth.security.UserSecurityStateService;
import com.riksa.auth.service.AuthService;
import com.riksa.auth.service.AccountDeletionService;
import com.riksa.auth.service.OAuth2Service;
import com.riksa.auth.telegram.PhoneVerificationService;
import com.riksa.auth.telegram.TelegramAuthFlowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;
    private AuthService authService = TestWebConfiguration.AUTH_SERVICE;
    private JwtService jwtService = TestWebConfiguration.JWT_SERVICE;
    private UserSecurityStateService securityStates = TestWebConfiguration.SECURITY_STATES;
    private AccountDeletionService accountDeletionService = TestWebConfiguration.ACCOUNT_DELETION_SERVICE;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.reset(authService, jwtService, securityStates, accountDeletionService);
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "auth-controller-test", Map.of("cors.allowed-origins", "http://localhost")));
        context.register(TestWebConfiguration.class);
        context.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void suspendedUserCannotReadProfileWithAValidBearerJwt() throws Exception {
        when(jwtService.isAccessTokenValid("suspended")).thenReturn(true);
        when(jwtService.getUserId("suspended")).thenReturn(USER_ID);
        when(securityStates.get(USER_ID)).thenReturn(new UserSecurityState(false, "USER"));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer suspended"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(authService);
    }

    @Test
    void profileUsesTheAuthenticatedPrincipal() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());

        mockMvc.perform(get("/api/v1/auth/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                .andExpect(status().isOk());

        verify(authService).getProfile(USER_ID);
        verifyNoInteractions(jwtService);
    }

    @Test
    void accountDeletionUsesTheAuthenticatedPrincipal() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());

        mockMvc.perform(delete("/api/v1/auth/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                .andExpect(status().isNoContent());

        verify(accountDeletionService).delete(USER_ID);
        verifyNoInteractions(jwtService);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestWebConfiguration {
        static final AuthService AUTH_SERVICE = mock(AuthService.class);
        static final JwtService JWT_SERVICE = mock(JwtService.class);
        static final UserSecurityStateService SECURITY_STATES = mock(UserSecurityStateService.class);
        static final AccountDeletionService ACCOUNT_DELETION_SERVICE = mock(AccountDeletionService.class);

        @Bean AuthController authController() {
            return new AuthController(
                    AUTH_SERVICE,
                    ACCOUNT_DELETION_SERVICE,
                    mock(OAuth2Service.class),
                    mock(TelegramAuthFlowService.class),
                    mock(PhoneVerificationService.class)
            );
        }
        @Bean JwtService jwtService() { return JWT_SERVICE; }
        @Bean UserSecurityStateService userSecurityStateService() {
            return SECURITY_STATES;
        }
        @Bean RedisRateLimiter redisRateLimiter() { return mock(RedisRateLimiter.class); }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(
                JwtService jwtService, UserSecurityStateService userSecurityStateService) {
            return new JwtAuthenticationFilter(jwtService, userSecurityStateService);
        }
        @Bean RateLimitFilter rateLimitFilter(RedisRateLimiter redisRateLimiter) {
            return new RateLimitFilter(redisRateLimiter, 5, 60, false);
        }
    }
}
