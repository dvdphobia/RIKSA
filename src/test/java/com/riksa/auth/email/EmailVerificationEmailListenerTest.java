package com.riksa.auth.email;

import com.riksa.auth.event.EmailVerificationRequestedEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class EmailVerificationEmailListenerTest {

    @Test
    void providerFailureDoesNotEscapeAfterCommit() {
        ResendEmailProperties properties = new ResendEmailProperties();
        properties.setEnabled(true);
        ResendEmailSender sender = mock(ResendEmailSender.class);

        doThrow(new IllegalStateException("provider failure")).when(sender)
                .sendVerificationEmail("user@example.com", "verification-token");
        EmailVerificationEmailListener listener = new EmailVerificationEmailListener(properties, sender);

        assertThatCode(() -> listener.sendAfterCommit(
                new EmailVerificationRequestedEvent(this, "user@example.com", "verification-token")
        )).doesNotThrowAnyException();

        verify(sender).sendVerificationEmail("user@example.com", "verification-token");
    }

    @Test
    void disabledDeliveryDoesNotCallProvider() {
        ResendEmailProperties properties = new ResendEmailProperties();
        ResendEmailSender sender = mock(ResendEmailSender.class);
        EmailVerificationEmailListener listener = new EmailVerificationEmailListener(properties, sender);

        listener.sendAfterCommit(new EmailVerificationRequestedEvent(this, "user@example.com", "token"));

        verify(sender, never()).sendVerificationEmail("user@example.com", "token");
    }
}
