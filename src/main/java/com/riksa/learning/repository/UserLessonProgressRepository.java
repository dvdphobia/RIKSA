package com.riksa.learning.repository;

import com.riksa.learning.entity.UserLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, UUID> {
    Optional<UserLessonProgress> findByUserIdAndLessonId(UUID userId, UUID lessonId);
    List<UserLessonProgress> findByUserIdAndLessonIdIn(UUID userId, List<UUID> lessonIds);
}
