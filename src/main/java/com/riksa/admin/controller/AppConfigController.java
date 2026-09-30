package com.riksa.admin.controller;

import com.riksa.admin.dto.settings.AppFlagsSettings;
import com.riksa.admin.service.SettingsService;
import com.riksa.common.constants.AdminEndpoints;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AdminEndpoints.APP_CONFIG)
@RequiredArgsConstructor
public class AppConfigController {

    private final SettingsService settingsService;

    @GetMapping
    public ResponseEntity<AppFlagsSettings> getConfig() {
        return ResponseEntity.ok(settingsService.getAppFlags());
    }
}
