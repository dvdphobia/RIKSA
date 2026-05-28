package com.riksa.auth;

public interface OtpService {

    String sendOtp(String phoneNumber);

    boolean verifyOtp(String phoneNumber, String code);
}
