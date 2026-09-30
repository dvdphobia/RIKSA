package com.riksa.quest.controller;

import com.riksa.auth.security.JwtAuthenticationFilter;
import com.riksa.auth.security.JwtService;
import com.riksa.auth.security.RateLimitFilter;
import com.riksa.auth.security.RedisRateLimiter;
import com.riksa.auth.security.SecurityConfig;
import com.riksa.auth.security.UserSecurityStateService;
import com.riksa.quest.dto.QuestChestResponse;
import com.riksa.quest.dto.WeekQuestsResponse;
import com.riksa.quest.dto.WeeklyQuestResponse;
import com.riksa.quest.service.QuestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestControllerTest {
    private static final UUID USER_ID = UUID.fromString("3eb0153c-7e8c-4c93-85f2-169093a328df");
    private static final UUID QUEST_ID = UUID.fromString("807e046d-4bbf-4c21-9b65-a7eeb4dafb2f");
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;
    private QuestService questService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "quest-controller-test", Map.of("cors.allowed-origins", "http://localhost")));
        context.register(TestWebConfiguration.class);
        context.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        questService = context.getBean(QuestService.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void weekRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/quests/week"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedLearnerGetsWeeklyQuestsAndChestJson() throws Exception {
        WeekQuestsResponse response = new WeekQuestsResponse(
                LocalDate.of(2026, 9, 28),
                OffsetDateTime.parse("2026-10-05T00:00:00+07:00"),
                List.of(new WeeklyQuestResponse(QUEST_ID, "WEEKLY_LESSONS", "Finish 7 lessons",
                        "បញ្ចប់មេរៀន 7", 7, 3, 100, false, false)),
                new QuestChestResponse(QUEST_ID, 9, 15, 21, false, 50, null));
        when(questService.getWeek(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/quests/week")
                        .with(SecurityMockMvcRequestPostProcessors.user(USER_ID.toString())))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "weekStart": "2026-09-28",
                          "resetsAt": "2026-10-05T00:00:00+07:00",
                          "quests": [{
                            "id": "807e046d-4bbf-4c21-9b65-a7eeb4dafb2f",
                            "type": "WEEKLY_LESSONS",
                            "title": "Finish 7 lessons",
                            "titleKm": "បញ្ចប់មេរៀន 7",
                            "target": 7,
                            "progress": 3,
                            "xpReward": 100,
                            "completed": false,
                            "claimed": false
                          }],
                          "chest": {
                            "id": "807e046d-4bbf-4c21-9b65-a7eeb4dafb2f",
                            "points": 9,
                            "target": 15,
                            "maxPoints": 21,
                            "opened": false,
                            "xpReward": 50,
                            "rewardItem": null
                          }
                        }
                        """));
        verify(questService).getWeek(USER_ID);
    }

    @Test
    void openingChestRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/quests/chest/open"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedLearnerOpensChestAndGetsChestJson() throws Exception {
        QuestChestResponse response = new QuestChestResponse(QUEST_ID, 15, 15, 21, true, 50, null);
        when(questService.openChest(USER_ID)).thenReturn(response);

        mockMvc.perform(post("/api/v1/quests/chest/open")
                        .with(SecurityMockMvcRequestPostProcessors.user(USER_ID.toString())))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "id": "807e046d-4bbf-4c21-9b65-a7eeb4dafb2f",
                          "points": 15,
                          "target": 15,
                          "maxPoints": 21,
                          "opened": true,
                          "xpReward": 50,
                          "rewardItem": null
                        }
                        """));
        verify(questService).openChest(USER_ID);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestWebConfiguration {
        @Bean QuestService questService() { return mock(QuestService.class); }
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
        @Bean QuestController questController(QuestService questService) {
            return new QuestController(questService);
        }
    }
}
