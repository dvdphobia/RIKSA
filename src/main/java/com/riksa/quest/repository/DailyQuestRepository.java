package com.riksa.quest.repository;
import com.riksa.quest.entity.DailyQuest;
import com.riksa.quest.entity.QuestType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface DailyQuestRepository extends JpaRepository<DailyQuest, UUID> {
    @Modifying
    @Query(value = """
            INSERT INTO daily_quest (id, user_id, quest_date, type, target, progress, xp_reward, claimed)
            VALUES (:id, :userId, :questDate, :type, :target, 0, :xpReward, FALSE)
            ON CONFLICT (user_id, quest_date, type) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId,
            @Param("questDate") LocalDate questDate, @Param("type") String type,
            @Param("target") int target, @Param("xpReward") int xpReward);
    List<DailyQuest> findByUserIdAndQuestDateOrderByTypeAsc(UUID userId, LocalDate questDate);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from DailyQuest q where q.id = :id and q.userId = :userId and q.questDate = :questDate")
    Optional<DailyQuest> findForUpdate(@Param("id") UUID id, @Param("userId") UUID userId,
            @Param("questDate") LocalDate questDate);
    @Modifying
    @Query("update DailyQuest q set q.progress = least(q.target, q.progress + 1) "
            + "where q.userId = :userId and q.questDate = :questDate "
            + "and q.type = :type and q.progress < q.target")
    int incrementProgress(@Param("userId") UUID userId, @Param("questDate") LocalDate questDate,
            @Param("type") QuestType type);
    long countByUserIdAndQuestDateAndClaimedFalse(UUID userId, LocalDate questDate);
}
