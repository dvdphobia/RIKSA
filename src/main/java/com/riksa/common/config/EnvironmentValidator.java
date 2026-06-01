package com.riksa.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class EnvironmentValidator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${cors.allowed-origins}")
    private String corsOrigins;

    @PostConstruct
    public void validate() {
        if (jwtSecret.equals("CHANGE-ME-IN-PRODUCTION-MUST-BE-256-BITS-OR-LONGER")) {
            log.warn("JWT_SECRET is using the default placeholder. Set a real secret before deploying!");
        }
        if (dbPassword == null || dbPassword.isBlank()) {
            log.warn("DB_PASSWORD is empty. Set a real password before deploying!");
        }
        if (corsOrigins.contains("xxxx")) {
            throw new IllegalStateException("CORS allowed-origins contains placeholder 'xxxx'. Fix CORS_ALLOWED_ORIGINS before starting.");
        }
        log.info("Environment validation passed");
    }
}
