package com.riksa.auth.email;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Getter
@Setter
@Slf4j
@Component
@ConfigurationProperties(prefix = "resend.email")
public class ResendEmailProperties {

    private boolean enabled = false;
    private String apiKey = "";
    private String from = "";
    private String confirmationUrl = "";

    @PostConstruct
    public void validateConfiguration() {
        if (!enabled) {
            log.info("Resend email delivery is disabled; verification emails will not be sent");
            return;
        }

        if (isBlank(apiKey) || isBlank(from) || isBlank(confirmationUrl)) {
            throw new IllegalStateException(
                    "Resend email delivery is enabled but RESEND_API_KEY, RESEND_FROM, or RESEND_CONFIRMATION_URL is missing"
            );
        }

        URI uri;
        try {
            uri = URI.create(confirmationUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("RESEND_CONFIRMATION_URL must be an absolute HTTPS URL");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalStateException(
                    "RESEND_CONFIRMATION_URL must be an absolute HTTPS page URL without credentials, query, or fragment"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
