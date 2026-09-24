package com.riksa.buddy.repository;

import com.riksa.buddy.entity.Buddy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BuddyRepository extends JpaRepository<Buddy, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO buddy (user_id, species, name, energy, bond, updated_at, created_at)
        VALUES (:userId, :species, :name, 60, 0, :now, :now)
        ON CONFLICT (user_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfMissing(
            @Param("userId") UUID userId,
            @Param("species") String species,
            @Param("name") String name,
            @Param("now") OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Buddy b where b.userId = :userId")
    Optional<Buddy> findByUserIdForUpdate(@Param("userId") UUID userId);
}

