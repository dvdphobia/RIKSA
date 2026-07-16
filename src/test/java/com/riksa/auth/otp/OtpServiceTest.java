package com.riksa.auth.otp;

import com.riksa.auth.telegram.TelegramService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock private OtpCodeRepository otpCodeRepository;
    @Mock private TelegramService telegramService;

    @InjectMocks private OtpService otpService;

    @Test
    void generateOtp_createsAndSendsCode() {
        ReflectionTestUtils.setField(otpService, "expirationMinutes", 5);
        ReflectionTestUtils.setField(otpService, "codeLength", 6);

        when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(inv -> inv.getArgument(0));

        String code = otpService.generateOtp("123456789");

        assertThat(code).hasSize(6);
        assertThat(code).matches("\\d{6}");
        verify(telegramService).sendOtp("123456789", code);
    }

    @Test
    void verifyOtp_validCode_returnsTrue() {
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);

        OtpCode otp = new OtpCode();
        otp.setIdentifier("123456789");
        otp.setCode("123456");
        otp.setVerified(false);
        otp.setAttempts(0);
        otp.setExpiresAt(OffsetDateTime.now().plusMinutes(5));

        when(otpCodeRepository.findTopByIdentifierAndChannelAndVerifiedFalseOrderByCreatedAtDesc(
                anyString(), eq(OtpChannel.TELEGRAM)))
                .thenReturn(Optional.of(otp));
        when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = otpService.verifyOtp("123456789", "123456");

        assertThat(result).isTrue();
        assertThat(otp.getVerified()).isTrue();
    }

    @Test
    void verifyOtp_invalidCode_returnsFalse() {
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);

        OtpCode otp = new OtpCode();
        otp.setIdentifier("123456789");
        otp.setCode("123456");
        otp.setVerified(false);
        otp.setAttempts(0);
        otp.setExpiresAt(OffsetDateTime.now().plusMinutes(5));

        when(otpCodeRepository.findTopByIdentifierAndChannelAndVerifiedFalseOrderByCreatedAtDesc(
                anyString(), eq(OtpChannel.TELEGRAM)))
                .thenReturn(Optional.of(otp));
        when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = otpService.verifyOtp("123456789", "wrong");

        assertThat(result).isFalse();
        assertThat(otp.getVerified()).isFalse();
        assertThat(otp.getAttempts()).isEqualTo(1);
    }

    @Test
    void verifyOtp_noPendingOtp_throwsException() {
        when(otpCodeRepository.findTopByIdentifierAndChannelAndVerifiedFalseOrderByCreatedAtDesc(
                anyString(), eq(OtpChannel.TELEGRAM)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verifyOtp("123456789", "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No pending OTP for this identifier");
    }

    @Test
    void verifyOtp_expiredOtp_throwsException() {
        OtpCode otp = new OtpCode();
        otp.setIdentifier("123456789");
        otp.setCode("123456");
        otp.setVerified(false);
        otp.setAttempts(0);
        otp.setExpiresAt(OffsetDateTime.now().minusMinutes(1));

        when(otpCodeRepository.findTopByIdentifierAndChannelAndVerifiedFalseOrderByCreatedAtDesc(
                anyString(), eq(OtpChannel.TELEGRAM)))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> otpService.verifyOtp("123456789", "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OTP has expired");
    }

    @Test
    void verifyOtp_tooManyAttempts_throwsException() {
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);

        OtpCode otp = new OtpCode();
        otp.setIdentifier("123456789");
        otp.setCode("123456");
        otp.setVerified(false);
        otp.setAttempts(3);
        otp.setExpiresAt(OffsetDateTime.now().plusMinutes(5));

        when(otpCodeRepository.findTopByIdentifierAndChannelAndVerifiedFalseOrderByCreatedAtDesc(
                anyString(), eq(OtpChannel.TELEGRAM)))
                .thenReturn(Optional.of(otp));
        when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> otpService.verifyOtp("123456789", "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Too many attempts. Please request a new OTP.");
    }
}