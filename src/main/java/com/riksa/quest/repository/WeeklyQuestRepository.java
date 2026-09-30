package com.riksa.quest.repository;

import com.riksa.quest.entity.WeeklyQuest;
import com.riksa.quest.entity.WeeklyQuestType;
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

public interface WeeklyQuestRepository extends JpaRepository<WeeklyQuest, UUID> {
    @Modifying
    @Query(value = """
            INSERT INTO weekly_quest (id, user_id, week_start, type, target, progress, xp_reward, claimed)
            VALUES (:id, :userId, :weekStart, :type, :target, 0, :xpReward, FALSE)
            ON CONFLICT (user_id, week_start, type) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId,
            @Param("weekStart") LocalDate weekStart, @Param("type") String type,
            @Param("target") int target, @Param("xpReward") int xpReward);

    List<WeeklyQuest> findByUserIdAndWeekStartOrderByTypeAsc(UUID userId, LocalDate weekStart);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from WeeklyQuest q where q.id = :id and q.userId = :userId and q.weekStart = :weekStart")
    Optional<WeeklyQuest> findForUpdate(@Param("id") UUID id, @Param("userId") UUID userId,
            @Param("weekStart") LocalDate weekStart);

    @Modifying
    @Query("update WeeklyQuest q set q.progress = least(q.target, q.progress + 1) "
            + "where q.userId = :userId and q.weekStart = :weekStart "
            + "and q.type = :type and q.progress < q.target")
    int incrementProgress(@Param("userId") UUID userId, @Param("weekStart") LocalDate weekStart,
            @Param("type") WeeklyQuestType type);

    @Modifying
    @Query(value = """
            UPDATE weekly_quest q
            SET progress = LEAST(q.target, (
                SELECT CAST(COUNT(DISTINCT CAST(
                    lc.completed_at AT TIME ZONE 'Asia/Phnom_Penh' AS date
                )) AS integer)
                FROM lesson_completions lc
                WHERE lc.user_id = :userId
                  AND CAST(lc.completed_at AT TIME ZONE 'Asia/Phnom_Penh' AS date) >= :weekStart
                  AND CAST(lc.completed_at AT TIME ZONE 'Asia/Phnom_Penh' AS date) < :weekEndExclusive
            ))
            WHERE q.user_id = :userId
              AND q.week_start = :weekStart
              AND q.type = 'WEEKLY_ACTIVE_DAYS'
            """, nativeQuery = true)
    int refreshActiveDaysProgress(@Param("userId") UUID userId, @Param("weekStart") LocalDate weekStart,
            @Param("weekEndExclusive") LocalDate weekEndExclusive);
}
