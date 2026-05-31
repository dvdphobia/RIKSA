package com.riksa.auth.controller;

import com.riksa.auth.dto.*;
import com.riksa.auth.google.GoogleAuthRequest;
import com.riksa.auth.otp.OtpChannel;
import com.riksa.auth.otp.OtpSendRequest;
import com.riksa.auth.otp.OtpVerifyRequest;
import com.riksa.auth.service.AuthService;
import com.riksa.auth.token.RefreshTokenRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(authService.googleAuth(request));
    }

    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, String>> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        String code = authService.sendOtp(request.getIdentifier(), request.getChannel());
        Map<String, String> response = Map.of(
                "message", "OTP sent successfully",
                "channel", request.getChannel().name(),
                "code", code
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        boolean verified = authService.verifyOtpOnly(request.getIdentifier(), request.getChannel(), request.getCode());
        return ResponseEntity.ok(Map.of(
                "verified", verified,
                "message", verified ? "Verification successful" : "Invalid OTP code"
        ));
    }
}
