package com.riksa.auth.controller;

import com.riksa.auth.dto.PublicProfileResponse;
import com.riksa.auth.service.PublicProfileService;
import com.riksa.common.constants.ProfileEndpoints;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ProfileEndpoints.BASE)
@RequiredArgsConstructor
public class PublicProfileController {

    private final PublicProfileService publicProfileService;

    @GetMapping(ProfileEndpoints.BY_USER_ID)
    public ResponseEntity<PublicProfileResponse> getProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(publicProfileService.getProfile(userId));
    }
}
