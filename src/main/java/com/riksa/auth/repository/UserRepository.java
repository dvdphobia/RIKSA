package com.riksa.auth.repository;

import com.riksa.auth.entity.Role;
import com.riksa.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByTelegramId(String telegramId);

    Optional<User> findByTelegramChatId(String telegramChatId);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByTelegramId(String telegramId);

    long countByCreatedAtAfter(OffsetDateTime cutoff);

    long countByLockedUntilAfter(OffsetDateTime cutoff);

    long countBySuspendedUntilAfter(OffsetDateTime cutoff);

    @Query("SELECT COALESCE(SUM(u.failedLoginAttempts), 0) FROM User u")
    long sumFailedLoginAttempts();

    @Query("""
        SELECT u FROM User u
        WHERE (LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%')))
          AND u.role = COALESCE(:role, u.role)
          AND (COALESCE(:suspended, FALSE) = FALSE OR u.suspendedUntil > :now)
          AND (COALESCE(:suspended, TRUE) = TRUE OR u.suspendedUntil IS NULL OR u.suspendedUntil <= :now)
        ORDER BY u.createdAt DESC, u.id DESC
    """)
    Page<User> search(@Param("query") String query,
                      @Param("role") Role role,
                      @Param("suspended") Boolean suspended,
                      @Param("now") OffsetDateTime now,
                      Pageable pageable);
}
