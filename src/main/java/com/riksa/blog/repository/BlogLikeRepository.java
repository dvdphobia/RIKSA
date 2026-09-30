package com.riksa.blog.repository;

import com.riksa.blog.entity.BlogLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BlogLikeRepository extends JpaRepository<BlogLike, UUID> {
    Optional<BlogLike> findByUserIdAndBlogPostId(UUID userId, UUID blogPostId);
    boolean existsByUserIdAndBlogPostId(UUID userId, UUID blogPostId);

    @Query("""
        SELECT COUNT(bl)
        FROM BlogLike bl
        JOIN BlogPost bp ON bl.blogPostId = bp.id
        WHERE bp.authorId = :authorId
        """)
    long countLikesOnUserBlogPosts(@Param("authorId") UUID authorId);
}