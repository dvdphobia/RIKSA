package com.riksa.auth;

import com.riksa.auth.entity.OtpVerification;
import com.riksa.auth.repository.OtpVerificationRepository;
import com.riksa.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsoleOtpService implements OtpService {

    private final OtpVerificationRepository otpRepository;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.otp.expiration-minutes:5}")
    private int expirationMinutes;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.otp.code-length:6}")
    private int codeLength;

    @Override
    public String sendOtp(String phoneNumber) {
        String normalized = PhoneUtils.normalize(phoneNumber);
        String code = generateCode();

        OtpVerification otp = OtpVerification.builder()
                .phoneNumber(normalized)
                .code(code)
                .verified(false)
                .attempts(0)
                .expiresAt(OffsetDateTime.now().plusMinutes(expirationMinutes))
                .build();

        otpRepository.save(otp);

        log.info("OTP for {}: {}", normalized, code);

        return code;
    }

    @Override
    public boolean verifyOtp(String phoneNumber, String code) {
        String normalized = PhoneUtils.normalize(phoneNumber);

        OtpVerification otp = otpRepository
                .findTopByPhoneNumberAndVerifiedFalseOrderByCreatedAtDesc(normalized)
                .orElseThrow(() -> new AppException("No pending OTP for this number", HttpStatus.BAD_REQUEST));

        if (otp.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new AppException("OTP has expired. Please request a new one", HttpStatus.BAD_REQUEST);
        }

        if (otp.getAttempts() >= maxAttempts) {
            throw new AppException("Too many attempts. Please request a new OTP", HttpStatus.TOO_MANY_REQUESTS);
        }

        otp.setAttempts(otp.getAttempts() + 1);

        if (!otp.getCode().equals(code)) {
            otpRepository.save(otp);
            throw new AppException("Invalid OTP code", HttpStatus.BAD_REQUEST);
        }

        otp.setVerified(true);
        otpRepository.save(otp);

        return true;
    }

    private String generateCode() {
        int upper = (int) Math.pow(10, codeLength);
        int lower = (int) Math.pow(10, codeLength - 1);
        int code = random.nextInt(upper - lower) + lower;
        return String.valueOf(code);
    }
}
