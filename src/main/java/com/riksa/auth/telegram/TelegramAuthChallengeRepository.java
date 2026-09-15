package com.riksa.auth.telegram;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelegramAuthChallengeRepository extends JpaRepository<TelegramAuthChallenge, UUID> {

    Optional<TelegramAuthChallenge> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM TelegramAuthChallenge c WHERE c.tokenHash = :tokenHash")
    Optional<TelegramAuthChallenge> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM TelegramAuthChallenge c WHERE c.startNonceHash = :nonceHash")
    Optional<TelegramAuthChallenge> findByStartNonceHashForUpdate(@Param("nonceHash") String nonceHash);

    void deleteByExpiresAtBefore(OffsetDateTime cutoff);
}

