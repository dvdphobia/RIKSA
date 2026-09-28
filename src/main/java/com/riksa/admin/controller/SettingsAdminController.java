package com.riksa.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.riksa.admin.service.SettingsService;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.ADMIN_SETTINGS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SettingsAdminController {

    private final SettingsService settingsService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        return ResponseEntity.ok(settingsService.getAll());
    }

    @PutMapping("/{key}")
    public ResponseEntity<Object> update(@PathVariable String key, @RequestBody JsonNode body) {
        return ResponseEntity.ok(settingsService.update(key, body, SecurityUtils.currentUserId()));
    }
}
