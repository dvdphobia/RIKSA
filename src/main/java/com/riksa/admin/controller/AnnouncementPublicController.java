package com.riksa.admin.controller;

import com.riksa.admin.dto.ActiveAnnouncementResponse;
import com.riksa.admin.service.AnnouncementAdminService;
import com.riksa.common.constants.AdminEndpoints;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(AdminEndpoints.PUBLIC_ANNOUNCEMENTS)
@RequiredArgsConstructor
public class AnnouncementPublicController {

    private final AnnouncementAdminService announcementAdminService;

    @GetMapping(AdminEndpoints.ACTIVE_ANNOUNCEMENTS)
    public ResponseEntity<List<ActiveAnnouncementResponse>> active() {
        return ResponseEntity.ok(announcementAdminService.active());
    }
}
