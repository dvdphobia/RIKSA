package com.riksa.blog.repository;

import com.riksa.blog.entity.BlogLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlogLikeRepository extends JpaRepository<BlogLike, UUID> {
    Optional<BlogLike> findByUserIdAndBlogPostId(UUID userId, UUID blogPostId);
    boolean existsByUserIdAndBlogPostId(UUID userId, UUID blogPostId);
}
