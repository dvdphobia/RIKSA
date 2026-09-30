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

    List<ExerciseAttempt> findByUserIdAndExerciseIdIn(UUID userId, List<UUID> exerciseIds);

    boolean existsByUserIdAndExerciseIdAndCorrectTrue(UUID userId, UUID exerciseId);

    long countByUserIdAndExerciseId(UUID userId, UUID exerciseId);

    long countByUserIdAndCorrectTrue(UUID userId);

    @Query("""
        SELECT e.lesson.id AS lessonId, COUNT(ea) AS correctCount
        FROM ExerciseAttempt ea
        JOIN Exercise e ON ea.exerciseId = e.id
        WHERE ea.userId = :userId AND ea.correct = true AND e.lesson.id IN :lessonIds
        GROUP BY e.lesson.id
    """)
    List<LessonCorrectCount> countCorrectByUserIdAndLessonIds(
            @Param("userId") UUID userId,
            @Param("lessonIds") List<UUID> lessonIds);

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
    interface LessonCorrectCount {
        UUID getLessonId();
        long getCorrectCount();
    }
}
