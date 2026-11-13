package com.riksa.auth.email;

import com.riksa.auth.event.EmailVerificationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationEmailListener {

    private final ResendEmailProperties properties;
    private final ResendEmailSender emailSender;

    @Async("resendEmailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAfterCommit(EmailVerificationRequestedEvent event) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            emailSender.sendVerificationEmail(event.getEmail(), event.getToken());
        } catch (RuntimeException exception) {
            // Do not log exception messages: provider errors can contain request details.
            log.error("Resend verification email failed after commit ({})",
                    exception.getClass().getSimpleName());
        }
    }
}
