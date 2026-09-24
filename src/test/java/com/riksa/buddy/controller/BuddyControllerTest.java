package com.riksa.buddy.controller;

import com.riksa.auth.security.JwtAuthenticationFilter;
import com.riksa.auth.security.JwtService;
import com.riksa.auth.security.RateLimitFilter;
import com.riksa.auth.security.RedisRateLimiter;
import com.riksa.auth.security.SecurityConfig;
import com.riksa.auth.security.UserSecurityStateService;
import com.riksa.buddy.dto.BuddyResponse;
import com.riksa.buddy.dto.BuddyTodayActionResponse;
import com.riksa.buddy.service.BuddyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BuddyControllerTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;
    private BuddyService buddyService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("buddy-controller-test",
                        Map.of("cors.allowed-origins", "http://localhost")));
        context.register(TestWebConfiguration.class);
        context.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        buddyService = context.getBean(BuddyService.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/buddy/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedLearnerCanReadBuddy() throws Exception {
        when(buddyService.getMe(USER_ID))
                .thenReturn(new BuddyResponse("crocodile", "Croco", 60, 0, 1,
                        OffsetDateTime.parse("2026-09-24T10:00:00Z"),
                        List.of(new BuddyTodayActionResponse("LESSON", false, 0, null))));

        mockMvc.perform(get("/api/v1/buddy/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(USER_ID.toString())))
                .andExpect(status().isOk());
        verify(buddyService).getMe(USER_ID);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestWebConfiguration {
        @Bean BuddyService buddyService() { return mock(BuddyService.class); }
        @Bean JwtService jwtService() { return mock(JwtService.class); }
        @Bean UserSecurityStateService userSecurityStateService() {
            return mock(UserSecurityStateService.class);
        }
        @Bean RedisRateLimiter redisRateLimiter() { return mock(RedisRateLimiter.class); }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(
                JwtService jwtService, UserSecurityStateService userSecurityStateService) {
            return new JwtAuthenticationFilter(jwtService, userSecurityStateService);
        }
        @Bean RateLimitFilter rateLimitFilter(RedisRateLimiter redisRateLimiter) {
            return new RateLimitFilter(redisRateLimiter, 5, 60, false);
        }
        @Bean BuddyController buddyController(BuddyService buddyService) {
            return new BuddyController(buddyService);
        }
    }
}
