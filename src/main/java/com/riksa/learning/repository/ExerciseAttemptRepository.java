package com.riksa.learning.repository;

import com.riksa.learning.entity.ExerciseAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttempt, UUID> {

    Optional<ExerciseAttempt> findByUserIdAndExerciseId(UUID userId, UUID exerciseId);

    List<ExerciseAttempt> findByUserIdAndExerciseIdIn(UUID userId, List<UUID> exerciseIds);

    long countByUserIdAndCorrectTrue(UUID userId);
}