package com.riksa.learning.repository;

import com.riksa.learning.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
    List<Exercise> findByLessonId(UUID lessonId);
    void deleteByLessonId(UUID lessonId);

    @Query("""
        SELECT e.lessonId AS lessonId, COUNT(e) AS exerciseCount
        FROM Exercise e
        WHERE e.lessonId IN :lessonIds
        GROUP BY e.lessonId
    """)
    List<LessonExerciseCount> countByLessonIds(@Param("lessonIds") List<UUID> lessonIds);

    interface LessonExerciseCount {
        UUID getLessonId();
        long getExerciseCount();
    }
}
