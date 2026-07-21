package com.riksa.learning.repository;

import com.riksa.common.enums.DifficultyLevel;
import com.riksa.learning.entity.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    @Query("""
        SELECT l FROM Lesson l
        WHERE (:categoryId IS NULL OR l.category.id = :categoryId)
          AND (:difficulty IS NULL OR l.difficulty = :difficulty)
        ORDER BY l.createdAt DESC
    """)
    Page<Lesson> findByFilters(@Param("categoryId") UUID categoryId,
                               @Param("difficulty") DifficultyLevel difficulty,
                               Pageable pageable);

    long countByCategoryId(UUID categoryId);
}
