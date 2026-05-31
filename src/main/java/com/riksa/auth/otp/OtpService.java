package com.riksa.auth.otp;

import com.riksa.auth.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final TelegramService telegramService;

    @Value("${otp.expiration-minutes}")
    private int expirationMinutes;

    @Value("${otp.max-attempts}")
    private int maxAttempts;

    @Value("${otp.code-length}")
    private int codeLength;

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateOtp(String identifier, OtpChannel channel) {
        String code = generateNumericCode();

        OtpCode otp = new OtpCode();
        otp.setIdentifier(identifier);
        otp.setChannel(channel);
        otp.setCode(code);
        otp.setExpiresAt(OffsetDateTime.now().plusMinutes(expirationMinutes));
        otp.setAttempts(0);
        otp.setVerified(false);

        otpCodeRepository.save(otp);

        // Send the OTP through the appropriate channel
        switch (channel) {
            case TELEGRAM -> telegramService.sendOtp(identifier, code);
            case SMS -> log.info("SMS OTP for {}: {} (SMS sending not yet configured)", identifier, code);
        }

        return code;
    }

    public boolean verifyOtp(String identifier, OtpChannel channel, String code) {
        OtpCode otp = otpCodeRepository
                .findTopByIdentifierAndChannelAndVerifiedFalseOrderByCreatedAtDesc(identifier, channel)
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
