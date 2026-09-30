package com.riksa.buddy.controller;

import com.riksa.buddy.dto.BuddyResponse;
import com.riksa.buddy.dto.BuddyEquipRequest;
import com.riksa.buddy.dto.BuddyUpdateRequest;
import com.riksa.buddy.service.BuddyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/buddy")
@RequiredArgsConstructor
public class BuddyController {

    private final BuddyService buddyService;

    @GetMapping("/me")
    public ResponseEntity<BuddyResponse> getMe() {
        return ResponseEntity.ok(buddyService.getMe(currentUserId()));
    }

    @PutMapping("/me")
    public ResponseEntity<BuddyResponse> update(@RequestBody @Valid BuddyUpdateRequest request) {
        return ResponseEntity.ok(buddyService.update(currentUserId(), request));
    }

    @PutMapping("/me/equipped")
    public ResponseEntity<BuddyResponse> equip(@RequestBody BuddyEquipRequest request) {
        return ResponseEntity.ok(buddyService.equip(currentUserId(), request));
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(authentication.getName());
    }
}
