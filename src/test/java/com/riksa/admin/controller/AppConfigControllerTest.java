package com.riksa.admin.controller;

import com.riksa.admin.dto.settings.AppFlagsSettings;
import com.riksa.admin.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppConfigControllerTest {

    private SettingsService settingsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        settingsService = mock(SettingsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AppConfigController(settingsService)).build();
    }

    @Test
    void getConfigReturnsCurrentAppFlagsAtPublicPath() throws Exception {
        when(settingsService.getAppFlags())
                .thenReturn(new AppFlagsSettings(true, "Maintenance", "1.4.0", "1.6.2"));

        mockMvc.perform(get("/api/v1/app/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintenanceMode").value(true))
                .andExpect(jsonPath("$.maintenanceMessage").value("Maintenance"))
                .andExpect(jsonPath("$.minAppVersion").value("1.4.0"))
                .andExpect(jsonPath("$.latestAppVersion").value("1.6.2"));
    }
}
