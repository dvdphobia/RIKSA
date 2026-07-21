package com.riksa.learning.repository;

import com.riksa.learning.entity.LessonCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonCompletionRepository extends JpaRepository<LessonCompletion, UUID> {

    Optional<LessonCompletion> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    boolean existsByUserIdAndLessonId(UUID userId, UUID lessonId);
}