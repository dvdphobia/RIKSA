package com.riksa.social.repository;

import com.riksa.social.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    @Query("""
        SELECT p FROM Post p
        WHERE (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:authorId IS NULL OR p.authorId = :authorId)
          AND (:cursor IS NULL
            OR p.createdAt < (SELECT p2.createdAt FROM Post p2 WHERE p2.id = :cursor)
            OR (p.createdAt = (SELECT p2.createdAt FROM Post p2 WHERE p2.id = :cursor)
                AND p.id < :cursor))
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    List<Post> findFeed(@Param("categoryId") UUID categoryId,
                        @Param("authorId") UUID authorId,
                        @Param("cursor") UUID cursor,
                        Pageable pageable);

    List<Post> findByAuthorId(UUID authorId);
    long countByAuthorId(UUID authorId);

    @Query("SELECT COALESCE(SUM(p.reactionCount), 0) FROM Post p WHERE p.authorId = :authorId")
    long sumReactionCountByAuthorId(@Param("authorId") UUID authorId);
}
