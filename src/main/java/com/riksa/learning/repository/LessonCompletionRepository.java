package com.riksa.learning.repository;

import com.riksa.learning.entity.LessonCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonCompletionRepository extends JpaRepository<LessonCompletion, UUID> {
    Optional<LessonCompletion> findByUserIdAndLessonId(UUID userId, UUID lessonId);
    List<LessonCompletion> findByUserIdAndLessonIdIn(UUID userId, List<UUID> lessonIds);
    long countByUserId(UUID userId);
    boolean existsByUserIdAndLessonId(UUID userId, UUID lessonId);

    @Query("""
        SELECT lc.lessonId AS lessonId,
               l.title AS lessonTitle,
               lc.xpEarned AS xpEarned,
               lc.score AS score,
               lc.completedAt AS completedAt
        FROM LessonCompletion lc
        LEFT JOIN Lesson l ON l.id = lc.lessonId
        WHERE lc.userId = :userId
        ORDER BY lc.completedAt DESC, lc.id DESC
        """)
    List<RecentCompletionRow> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    interface RecentCompletionRow {
        UUID getLessonId();
        String getLessonTitle();
        int getXpEarned();
        int getScore();
        java.time.OffsetDateTime getCompletedAt();
    }

    long countByCompletedAtAfter(java.time.OffsetDateTime cutoff);

    @Query("""
        SELECT COUNT(DISTINCT l.category.id)
        FROM LessonCompletion lc
        JOIN Lesson l ON lc.lessonId = l.id
        WHERE lc.userId = :userId AND l.category.id IS NOT NULL
        """)
    long countDistinctCategoriesCompleted(@Param("userId") UUID userId);

    @Query("""
        SELECT COUNT(lc)
        FROM LessonCompletion lc
        JOIN Lesson l ON lc.lessonId = l.id
        WHERE lc.userId = :userId AND l.category.id = :categoryId
        """)
    long countCompletedInCategory(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId);

    @Query("""
        SELECT l.category.id AS categoryId, COUNT(lc) AS completedCount
        FROM LessonCompletion lc
        JOIN Lesson l ON lc.lessonId = l.id
        WHERE lc.userId = :userId AND l.category.id IS NOT NULL
        GROUP BY l.category.id
        """)
    List<CategoryCompletionCount> countCompletionsByCategory(@Param("userId") UUID userId);

    interface CategoryCompletionCount {
        UUID getCategoryId();
        long getCompletedCount();
    }
}
