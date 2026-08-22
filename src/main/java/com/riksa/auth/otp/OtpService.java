package com.riksa.auth.otp;

import com.riksa.auth.security.RateLimitDecision;
import com.riksa.auth.security.RedisRateLimiter;
import com.riksa.auth.telegram.TelegramService;
import com.riksa.common.config.RiksaRedisProperties;
import com.riksa.common.exception.RateLimitExceededException;
import com.riksa.common.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final TelegramService telegramService;
    private final RedisOtpStore redisOtpStore;
    private final OtpHasher otpHasher;
    private final RedisRateLimiter rateLimiter;
    private final RiksaRedisProperties redisProperties;

    @Value("${otp.expiration-minutes}")
    private int expirationMinutes;

    @Value("${otp.max-attempts}")
    private int maxAttempts;

    @Value("${otp.code-length}")
    private int codeLength;

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateOtp(String identifier) {
        RateLimitDecision decision;
        try {
            decision = rateLimiter.acquire(
                    "otp-send-identifier",
                    OtpChannel.TELEGRAM.name() + ":" + identifier,
                    3,
                    Duration.ofMinutes(10)
            );
        } catch (RuntimeException e) {
            throw new ServiceUnavailableException("OTP service is temporarily unavailable", e);
        }
        if (!decision.allowed()) {
            throw new RateLimitExceededException(
                    "Too many OTP requests. Try again later.", decision.retryAfterSeconds());
        }

        String code = generateNumericCode();

        if (redisProperties.isOtpEnabled()) {
            try {
                redisOtpStore.save(
                        identifier,
                        OtpChannel.TELEGRAM,
                        otpHasher.hash(identifier, code),
                        Duration.ofMinutes(expirationMinutes)
                );
            } catch (RuntimeException e) {
                throw new ServiceUnavailableException("OTP service is temporarily unavailable", e);
            }
            telegramService.sendOtp(identifier, code);
            return code;
        }

        OtpCode otp = new OtpCode();
        otp.setIdentifier(identifier);
        otp.setChannel(OtpChannel.TELEGRAM);
        otp.setCode(code);
        otp.setExpiresAt(OffsetDateTime.now().plusMinutes(expirationMinutes));
        otp.setAttempts(0);
        otp.setVerified(false);

        otpCodeRepository.save(otp);
        telegramService.sendOtp(identifier, code);

        return code;
    }

    public boolean verifyOtp(String identifier, String code) {
        if (redisProperties.isOtpEnabled()) {
            OtpVerificationResult result;
            try {
                result = redisOtpStore.verify(
                        identifier,
                        OtpChannel.TELEGRAM,
                        otpHasher.hash(identifier, code),
                        maxAttempts
                );
            } catch (RuntimeException e) {
                throw new ServiceUnavailableException("OTP service is temporarily unavailable", e);
            }
            switch (result) {
                case VERIFIED -> { return true; }
                case INVALID -> { return false; }
                case TOO_MANY_ATTEMPTS -> throw new IllegalArgumentException(
                        "Too many attempts. Please request a new OTP.");
                case NOT_FOUND -> {
                    // Temporary migration fallback for OTPs created before Redis deployment.
                }
            }
        }

        return verifyLegacyOtp(identifier, code);
    }

    private boolean verifyLegacyOtp(String identifier, String code) {
        OtpCode otp = otpCodeRepository
                .findTopByIdentifierAndChannelAndVerifiedFalseOrderByCreatedAtDesc(identifier, OtpChannel.TELEGRAM)
                .orElseThrow(() -> new IllegalArgumentException("No pending OTP for this identifier"));

        if (otp.getVerified()) {
            throw new IllegalArgumentException("OTP already verified");
        }

        if (otp.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired");
        }

        otp.setAttempts(otp.getAttempts() + 1);

        if (otp.getAttempts() > maxAttempts) {
            otpCodeRepository.save(otp);
            throw new IllegalArgumentException("Too many attempts. Please request a new OTP.");
        }

        if (!otp.getCode().equals(code)) {
            otpCodeRepository.save(otp);
            return false;
        }

        otp.setVerified(true);
        otpCodeRepository.save(otp);
        return true;
    }

    private String generateNumericCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
