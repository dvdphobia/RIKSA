package com.riksa.social.repository;

import com.riksa.social.entity.Post;
import com.riksa.social.enums.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    @Query("""
        SELECT p FROM Post p
        WHERE p.status = :status
          AND p.categoryId = COALESCE(:categoryId, p.categoryId)
          AND p.authorId = COALESCE(:authorId, p.authorId)
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    List<Post> findFeedFirstPage(@Param("categoryId") UUID categoryId,
                                 @Param("authorId") UUID authorId,
                                 @Param("status") ContentStatus status,
                                 Pageable pageable);

    @Query("""
        SELECT p FROM Post p
        WHERE p.status = :status
          AND p.categoryId = COALESCE(:categoryId, p.categoryId)
          AND p.authorId = COALESCE(:authorId, p.authorId)
          AND (p.createdAt < (SELECT p2.createdAt FROM Post p2 WHERE p2.id = :cursor)
            OR (p.createdAt = (SELECT p2.createdAt FROM Post p2 WHERE p2.id = :cursor)
                AND p.id < :cursor))
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    List<Post> findFeedAfterCursor(@Param("categoryId") UUID categoryId,
                                   @Param("authorId") UUID authorId,
                                   @Param("cursor") UUID cursor,
                                   @Param("status") ContentStatus status,
                                   Pageable pageable);

    Page<Post> findByStatus(ContentStatus status, Pageable pageable);

    List<Post> findByAuthorId(UUID authorId);
    long countByAuthorId(UUID authorId);

    long countByAuthorIdAndCreatedAtAfter(UUID authorId, OffsetDateTime cutoff);

    List<Post> findByAuthorIdAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
            UUID authorId, OffsetDateTime cutoff, Pageable pageable);

    long countByCreatedAtAfter(java.time.OffsetDateTime cutoff);

    @Query("SELECT COALESCE(SUM(p.reactionCount), 0) FROM Post p WHERE p.authorId = :authorId")
    long sumReactionCountByAuthorId(@Param("authorId") UUID authorId);
}
