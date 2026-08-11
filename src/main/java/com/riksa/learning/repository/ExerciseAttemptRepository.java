package com.riksa.learning.repository;

import com.riksa.learning.entity.ExerciseAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttempt, UUID> {

    Optional<ExerciseAttempt> findByUserIdAndExerciseId(UUID userId, UUID exerciseId);

    List<ExerciseAttempt> findByUserIdAndExerciseIdIn(UUID userId, List<UUID> exerciseIds);

    long countByUserIdAndCorrectTrue(UUID userId);

    @Query("""
        SELECT ea.exerciseId AS exerciseId,
               COUNT(ea) AS attempts,
               SUM(CASE WHEN ea.correct = true THEN 1 ELSE 0 END) AS correctCount
        FROM ExerciseAttempt ea
        GROUP BY ea.exerciseId
        HAVING COUNT(ea) >= :minAttempts
    """)
    List<ExerciseAttemptStats> findAttemptStats(@Param("minAttempts") long minAttempts);

    interface ExerciseAttemptStats {
        UUID getExerciseId();
        long getAttempts();
        long getCorrectCount();
    }
}