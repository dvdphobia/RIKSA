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
import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    @Query("""
        SELECT l FROM Lesson l
        WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY l.createdAt DESC
    """)
    Page<Lesson> searchByTitle(@Param("query") String query, Pageable pageable);

    @Query("""
        SELECT l FROM Lesson l
        WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%'))
          AND l.difficulty = :difficulty
        ORDER BY l.createdAt DESC
    """)
    Page<Lesson> searchByTitleAndDifficulty(@Param("query") String query,
                                            @Param("difficulty") DifficultyLevel difficulty,
                                            Pageable pageable);

    @Query("""
        SELECT l FROM Lesson l
        WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%'))
          AND l.category.id = :categoryId
        ORDER BY l.createdAt DESC
    """)
    Page<Lesson> searchByTitleAndCategoryId(@Param("query") String query,
                                            @Param("categoryId") UUID categoryId,
                                            Pageable pageable);

    @Query("""
        SELECT l FROM Lesson l
        WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%'))
          AND l.difficulty = :difficulty
          AND l.category.id = :categoryId
        ORDER BY l.createdAt DESC
    """)
    Page<Lesson> searchByTitleAndDifficultyAndCategoryId(@Param("query") String query,
                                                         @Param("difficulty") DifficultyLevel difficulty,
                                                         @Param("categoryId") UUID categoryId,
                                                         Pageable pageable);

    Page<Lesson> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Lesson> findByCategoryIdOrderByCreatedAtDesc(UUID categoryId, Pageable pageable);
    Page<Lesson> findByDifficultyOrderByCreatedAtDesc(DifficultyLevel difficulty, Pageable pageable);
    Page<Lesson> findByCategoryIdAndDifficultyOrderByCreatedAtDesc(
            UUID categoryId, DifficultyLevel difficulty, Pageable pageable);

    long countByCategoryId(UUID categoryId);

    @Query("""
        SELECT l FROM Lesson l
        JOIN FETCH l.roadmapUnit ru
        JOIN FETCH ru.category
        WHERE ru.id IN :roadmapUnitIds
        ORDER BY ru.position ASC, l.position ASC
    """)
    List<Lesson> findRoadmapLessons(@Param("roadmapUnitIds") List<UUID> roadmapUnitIds);
}
