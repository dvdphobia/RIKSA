package com.riksa.quest.repository;

import com.riksa.quest.entity.QuestChest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface QuestChestRepository extends JpaRepository<QuestChest, UUID> {
    @Modifying
    @Query(value = """
            INSERT INTO quest_chest (id, user_id, week_start)
            VALUES (:id, :userId, :weekStart)
            ON CONFLICT (user_id, week_start) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId,
            @Param("weekStart") LocalDate weekStart);

    Optional<QuestChest> findByUserIdAndWeekStart(UUID userId, LocalDate weekStart);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from QuestChest c where c.id = :id and c.userId = :userId and c.weekStart = :weekStart")
    Optional<QuestChest> findForUpdate(@Param("id") UUID id, @Param("userId") UUID userId,
            @Param("weekStart") LocalDate weekStart);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from QuestChest c where c.userId = :userId and c.weekStart = :weekStart")
    Optional<QuestChest> findForUpdate(@Param("userId") UUID userId, @Param("weekStart") LocalDate weekStart);
}
