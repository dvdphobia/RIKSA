package com.riksa.auth.telegram;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "telegram_auth_challenges")
@Getter
@Setter
public class TelegramAuthChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** HMAC of the deep-link nonce or of the bot-issued sign-in code. */
    @Column(name = "start_nonce_hash", unique = true, length = 64)
    private String startNonceHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TelegramAuthChallengeStatus status = TelegramAuthChallengeStatus.PENDING;

    @Column(name = "telegram_user_id", columnDefinition = "TEXT")
    private String telegramUserId;

    @Column(name = "telegram_chat_id", columnDefinition = "TEXT")
    private String telegramChatId;

    @Column(name = "telegram_username", length = 64)
    private String telegramUsername;

    @Column(name = "telegram_display_name", length = 100)
    private String telegramDisplayName;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

