package com.riksa.auth.controller;

import com.riksa.auth.dto.PublicProfileResponse;
import com.riksa.auth.service.PublicProfileService;
import com.riksa.common.exception.GlobalExceptionHandler;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.gamification.dto.GamificationProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicProfileControllerTest {

    private PublicProfileService publicProfileService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        publicProfileService = mock(PublicProfileService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PublicProfileController(publicProfileService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getProfile_returnsPublicProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        GamificationProfileResponse gamification = new GamificationProfileResponse();
        gamification.setUserId(userId);
        gamification.setTotalXp(750);
        gamification.setCurrentTier("GOLD");
        PublicProfileResponse response = new PublicProfileResponse(
                userId,
                "sophea",
                "Sok Sophea",
                "/avatars/avatar-17.png",
                "Learning STEM & Tech",
                "USER",
                "KH",
                null,
                OffsetDateTime.parse("2026-08-01T10:15:30+07:00"),
                gamification
        );
        when(publicProfileService.getProfile(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/profiles/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("sophea"))
                .andExpect(jsonPath("$.gamification.totalXp").value(750))
                .andExpect(jsonPath("$.gamification.currentTier").value("GOLD"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist());
    }

    @Test
    void getProfile_unknownUser_returnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        when(publicProfileService.getProfile(userId))
                .thenThrow(new ResourceNotFoundException("User not found: " + userId));

        mockMvc.perform(get("/api/v1/profiles/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found: " + userId));
    }

    @Test
    void getProfile_invalidUserId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'userId'"));
    }
}
