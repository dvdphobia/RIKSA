package com.riksa.report.controller;

import com.riksa.common.constants.ReportEndpoints;
import com.riksa.report.dto.CreateReportRequest;
import com.riksa.report.dto.ReportResponse;
import com.riksa.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ReportEndpoints.BASE)
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping(ReportEndpoints.ROOT)
    public ResponseEntity<ReportResponse> fileReport(@RequestBody @Valid CreateReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.fileReport(getCurrentUserId(), request));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}
