package com.riksa.admin.controller;

import com.riksa.admin.dto.ChatSessionDetailResponse;
import com.riksa.admin.dto.ChatSessionSummaryResponse;
import com.riksa.admin.dto.ChatUsageResponse;
import com.riksa.admin.service.ChatAiAdminService;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.common.security.SecurityUtils;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.ADMIN_AI_CHAT)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Validated
public class ChatAiAdminController {

    private final ChatAiAdminService chatAiAdminService;

    @GetMapping(AdminEndpoints.ADMIN_AI_CHAT_USAGE)
    public ResponseEntity<ChatUsageResponse> usage(
            @RequestParam(defaultValue = "1") @Min(1) @Max(365) int days) {
        return ResponseEntity.ok(chatAiAdminService.usage(days));
    }

    @GetMapping(AdminEndpoints.ADMIN_AI_CHAT_USER_SESSIONS)
    public ResponseEntity<List<ChatSessionSummaryResponse>> sessions(@PathVariable UUID userId) {
        return ResponseEntity.ok(chatAiAdminService.sessions(userId));
    }

    @GetMapping(AdminEndpoints.ADMIN_AI_CHAT_SESSION)
    public ResponseEntity<ChatSessionDetailResponse> transcript(
            @PathVariable UUID id,
            @RequestParam @NotBlank @Size(max = 500) String reason) {
        return ResponseEntity.ok(chatAiAdminService.transcript(SecurityUtils.currentUserId(), id, reason));
    }
}
