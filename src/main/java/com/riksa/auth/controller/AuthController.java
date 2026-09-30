package com.riksa.auth.controller;

import com.riksa.auth.dto.*;
import com.riksa.auth.google.GoogleAuthRequest;
import com.riksa.auth.otp.OtpSendRequest;
import com.riksa.auth.otp.OtpVerifyRequest;
import com.riksa.auth.telegram.PhoneVerificationService;
import com.riksa.auth.telegram.TelegramAuthCompleteRequest;
import com.riksa.auth.telegram.TelegramAuthFlowService;
import com.riksa.auth.telegram.TelegramAuthRequest;
import com.riksa.auth.telegram.TelegramAuthStartResponse;
import com.riksa.auth.telegram.TelegramAuthResult;
import com.riksa.auth.telegram.TelegramAuthStatusRequest;
import com.riksa.auth.telegram.TelegramBotInfoResponse;
import com.riksa.auth.telegram.TelegramCodeRequest;
import com.riksa.auth.telegram.TelegramLinkRequest;
import com.riksa.auth.telegram.TelegramUpdate;
import com.riksa.auth.security.JwtService;
import com.riksa.auth.service.AuthService;
import com.riksa.auth.service.OAuth2Service;
import com.riksa.auth.token.RefreshTokenRequest;
import com.riksa.common.constants.AuthEndpoints;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(AuthEndpoints.BASE)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuth2Service oAuth2Service;
    private final JwtService jwtService;
    private final TelegramAuthFlowService telegramAuthFlowService;
    private final PhoneVerificationService phoneVerificationService;

    @PostMapping(AuthEndpoints.REGISTER)
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping(AuthEndpoints.LOGIN)
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping(AuthEndpoints.REFRESH)
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping(AuthEndpoints.LOGOUT)
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @PostMapping(AuthEndpoints.GOOGLE)
    public ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(oAuth2Service.googleAuth(request));
    }

    @PostMapping(AuthEndpoints.TELEGRAM)
    public ResponseEntity<AuthResponse> telegramAuth(@Valid @RequestBody TelegramAuthRequest request) {
        return ResponseEntity.ok(authService.telegramAuth(request));
    }

    @GetMapping(AuthEndpoints.TELEGRAM_BOT)
    public ResponseEntity<TelegramBotInfoResponse> telegramBotInfo() {
        return ResponseEntity.ok(telegramAuthFlowService.botInfo());
    }

    @PostMapping(AuthEndpoints.TELEGRAM_START)
    public ResponseEntity<TelegramAuthStartResponse> startTelegramAuth() {
        return ResponseEntity.ok(telegramAuthFlowService.start());
    }

    @PostMapping(AuthEndpoints.TELEGRAM_STATUS)
    public ResponseEntity<TelegramAuthResult> getTelegramAuthStatus(
            @Valid @RequestBody TelegramAuthStatusRequest request
    ) {
        return ResponseEntity.ok(telegramAuthFlowService.status(request.requestToken()));
    }

    @PostMapping(AuthEndpoints.TELEGRAM_CODE)
    public ResponseEntity<TelegramAuthResult> redeemTelegramCode(@Valid @RequestBody TelegramCodeRequest request) {
        return ResponseEntity.ok(telegramAuthFlowService.redeemCode(request.code()));
    }

    @PostMapping(AuthEndpoints.TELEGRAM_COMPLETE)
    public ResponseEntity<AuthResponse> completeTelegramSignup(
            @Valid @RequestBody TelegramAuthCompleteRequest request
    ) {
        return ResponseEntity.ok(telegramAuthFlowService.completeSignup(request));
    }

    @PostMapping(AuthEndpoints.TELEGRAM_LINK)
    public ResponseEntity<MessageResponse> linkTelegram(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody TelegramLinkRequest request
    ) {
        telegramAuthFlowService.link(extractUserId(authHeader), request);
        return ResponseEntity.ok(new MessageResponse("Telegram account linked successfully"));
    }

    /** Returns the bot link that verifies the signed-in user's phone through Telegram. */
    @PostMapping(AuthEndpoints.PHONE_VERIFY_START)
    public ResponseEntity<PhoneVerificationService.StartResponse> startPhoneVerification(
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(phoneVerificationService.start(extractUserId(authHeader)));
    }

    @PostMapping(AuthEndpoints.TELEGRAM_WEBHOOK)
    public ResponseEntity<Void> telegramWebhook(
            @RequestHeader(name = "X-Telegram-Bot-Api-Secret-Token", required = false) String webhookSecret,
            @RequestBody TelegramUpdate update
    ) {
        telegramAuthFlowService.handleWebhook(webhookSecret, update);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(AuthEndpoints.OTP_SEND)
    public ResponseEntity<Map<String, String>> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        authService.sendOtp(request.getIdentifier());
        return ResponseEntity.ok(Map.of(
                "message", "OTP sent successfully",
                "channel", "TELEGRAM"
        ));
    }

    @PostMapping(AuthEndpoints.OTP_VERIFY)
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        boolean verified = authService.verifyOtp(request.getIdentifier(), request.getCode());
        return ResponseEntity.ok(Map.of(
                "verified", verified,
                "message", verified ? "Verification successful" : "Invalid OTP code"
        ));
    }

    @GetMapping(AuthEndpoints.ME)
    public ResponseEntity<UserProfileResponse> getProfile(@RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(authService.getProfile(userId));
    }

    @GetMapping(AuthEndpoints.PROVINCES)
    public ResponseEntity<List<ProvinceResponse>> getProvinces() {
        return ResponseEntity.ok(authService.getProvinces());
    }

    @PutMapping(AuthEndpoints.ME)
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UUID userId = extractUserId(authHeader);
        return ResponseEntity.ok(authService.updateProfile(
                userId,
                request.getDisplayName(),
                request.getBio(),
                request.getGender(),
                request.getProvinceId(),
                request.getAvatarUrl(),
                request.getShareMilestones()
        ));
    }

    @PostMapping(AuthEndpoints.FORGOT_PASSWORD)
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getIdentifier());
        return ResponseEntity.ok(new MessageResponse("If an account exists with that identifier, an OTP has been sent"));
    }

    @PostMapping(AuthEndpoints.RESET_PASSWORD)
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getIdentifier(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(new MessageResponse("Password has been reset successfully"));
    }

    @PostMapping(AuthEndpoints.VERIFY_EMAIL)
    public ResponseEntity<Map<String, Object>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        boolean verified = authService.verifyEmail(request.getToken());
        return ResponseEntity.ok(Map.of(
                "verified", verified,
                "message", verified ? "Email verified successfully" : "Verification failed"
        ));
    }

    private UUID extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        return jwtService.getUserId(token);
    }
}
