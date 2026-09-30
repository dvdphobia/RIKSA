package com.riksa.common.scheduler;

import com.riksa.auth.otp.OtpCodeRepository;
import com.riksa.auth.token.EmailVerificationTokenRepository;
import com.riksa.auth.token.RefreshTokenRepository;
import com.riksa.auth.telegram.TelegramAuthChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class TokenCleanupTask {

    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final TelegramAuthChallengeRepository telegramAuthChallengeRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "token-cleanup", lockAtMostFor = "30m", lockAtLeastFor = "1m")
    @Transactional
    public void cleanExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
        otpCodeRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
        emailVerificationTokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
        telegramAuthChallengeRepository.deleteByExpiresAtBefore(OffsetDateTime.now().minusDays(1));
    }
}
