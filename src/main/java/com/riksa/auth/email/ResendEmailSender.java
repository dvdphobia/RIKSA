package com.riksa.auth.email;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class ResendEmailSender {

    private final RestClient restClient;
    private final ResendEmailProperties properties;

    public ResendEmailSender(
            @Qualifier("resendRestClient") RestClient restClient,
            ResendEmailProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public void sendVerificationEmail(String recipient, String token) {
        // Keep the token in the fragment: browsers do not send fragments in the page request.
        String confirmationLink = properties.getConfirmationUrl() + "#token=" + token;

        String escapedLink = HtmlUtils.htmlEscape(confirmationLink);
        String text = "Confirm your Riksa email address by opening this link:\n\n" + confirmationLink;
        String html = """
                <!doctype html>
                <html lang="en">
                <body>
                  <p>Confirm your Riksa email address:</p>
                  <p><a href="%s">Confirm email</a></p>
                  <p>If you did not create a Riksa account, you can ignore this message.</p>
                </body>
                </html>
                """.formatted(escapedLink);

        restClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Idempotency-Key", idempotencyKey(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "from", properties.getFrom(),
                        "to", List.of(recipient),
                        "subject", "Verify your Riksa email address",
                        "html", html,
                        "text", text
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private String idempotencyKey(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return "email-verification-" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

}
