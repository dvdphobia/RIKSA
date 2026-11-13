package com.riksa.auth.email;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendEmailSenderTest {

    @Test
    void sendsVerificationMessageThroughMockedResendApi() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.resend.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        ResendEmailProperties properties = new ResendEmailProperties();
        properties.setApiKey("unit-test-api-key");
        properties.setFrom("Riksa <no-reply@example.com>");
        properties.setConfirmationUrl("https://api.example.com/email-verification.html");

        ResendEmailSender sender = new ResendEmailSender(builder.build(), properties);
        String expectedKey = "email-verification-" + HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest("verification-token".getBytes(StandardCharsets.UTF_8))
        );

        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer unit-test-api-key"))
                .andExpect(header("Idempotency-Key", equalTo(expectedKey)))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.from", equalTo("Riksa <no-reply@example.com>")))
                .andExpect(jsonPath("$.to[0]", equalTo("user@example.com")))
                .andExpect(jsonPath("$.subject", equalTo("Verify your Riksa email address")))
                .andExpect(jsonPath("$.html", containsString(
                        "https://api.example.com/email-verification.html#token=verification-token"
                )))
                .andExpect(jsonPath("$.text", containsString(
                        "https://api.example.com/email-verification.html#token=verification-token"
                )))
                .andRespond(withSuccess("{\"id\":\"local-test\"}", MediaType.APPLICATION_JSON));

        sender.sendVerificationEmail("user@example.com", "verification-token");

        server.verify();
    }
}
