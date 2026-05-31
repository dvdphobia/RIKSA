package com.riksa.auth.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findTopByIdentifierAndChannelAndVerifiedFalseOrderByCreatedAtDesc(
            String identifier, OtpChannel channel);
}
