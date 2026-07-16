package com.riksa.learning.repository;

import com.riksa.learning.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
    List<Exercise> findByLessonId(UUID lessonId);
    void deleteByLessonId(UUID lessonId);
}