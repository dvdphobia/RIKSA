package com.riksa.learning.repository;

import com.riksa.common.enums.DifficultyLevel;
import com.riksa.learning.entity.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    @Query("""
        SELECT l FROM Lesson l
        WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY l.position ASC
    """)
    Page<Lesson> searchByTitle(@Param("query") String query, Pageable pageable);

    @Query("""
        SELECT l FROM Lesson l
        WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%'))
          AND l.difficulty = :difficulty
        ORDER BY l.position ASC
    """)
    Page<Lesson> searchByTitleAndDifficulty(@Param("query") String query,
                                            @Param("difficulty") DifficultyLevel difficulty,
                                            Pageable pageable);

    @Query("""
        SELECT l FROM Lesson l
        WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%'))
          AND l.category.id = :categoryId
        ORDER BY l.position ASC
    """)
    Page<Lesson> searchByTitleAndCategoryId(@Param("query") String query,
                                            @Param("categoryId") UUID categoryId,
                                            Pageable pageable);

    @Query("""
        SELECT l FROM Lesson l
        WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%'))
          AND l.difficulty = :difficulty
          AND l.category.id = :categoryId
        ORDER BY l.position ASC
    """)
    Page<Lesson> searchByTitleAndDifficultyAndCategoryId(@Param("query") String query,
                                                         @Param("difficulty") DifficultyLevel difficulty,
                                                         @Param("categoryId") UUID categoryId,
                                                         Pageable pageable);

    Page<Lesson> findAllByOrderByPositionAsc(Pageable pageable);
    Page<Lesson> findByCategoryIdOrderByPositionAsc(UUID categoryId, Pageable pageable);
    Page<Lesson> findByDifficultyOrderByPositionAsc(DifficultyLevel difficulty, Pageable pageable);
    Page<Lesson> findByCategoryIdAndDifficultyOrderByPositionAsc(
            UUID categoryId, DifficultyLevel difficulty, Pageable pageable);

    List<Lesson> findByCategoryIdOrderByPositionAsc(UUID categoryId);
    List<Lesson> findByCategoryIsNullOrderByPositionAsc();
    Optional<Lesson> findFirstByCategoryIdOrderByPositionDesc(UUID categoryId);
    Optional<Lesson> findFirstByCategoryIsNullOrderByPositionDesc();

    @Query("""
        SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END
        FROM Lesson l
        WHERE l.position = :position
          AND ((:categoryId IS NULL AND l.category IS NULL) OR l.category.id = :categoryId)
          AND (:excludeId IS NULL OR l.id <> :excludeId)
    """)
    boolean existsAtPosition(@Param("categoryId") UUID categoryId,
                             @Param("position") int position,
                             @Param("excludeId") UUID excludeId);

    long countByCategoryId(UUID categoryId);

    long countByRoadmapUnit_Id(UUID roadmapUnitId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Lesson l SET l.roadmapUnit = null WHERE l.roadmapUnit.id = :roadmapUnitId")
    int clearRoadmapUnit(@Param("roadmapUnitId") UUID roadmapUnitId);

    @Query("""
        SELECT l FROM Lesson l
        JOIN FETCH l.roadmapUnit ru
        JOIN FETCH ru.category
        WHERE ru.id IN :roadmapUnitIds
        ORDER BY ru.position ASC, l.position ASC
    """)
    List<Lesson> findRoadmapLessons(@Param("roadmapUnitIds") List<UUID> roadmapUnitIds);
}
