package com.riksa.blog.repository;

import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {
    Page<BlogPost> findByStatus(BlogStatus status, Pageable pageable);
    long countByStatus(BlogStatus status);
    Page<BlogPost> findByStatusAndCategoryId(BlogStatus status, UUID categoryId, Pageable pageable);
    Page<BlogPost> findByAuthorId(UUID authorId, Pageable pageable);
    Page<BlogPost> findByAuthorIdAndStatus(UUID authorId, BlogStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bp FROM BlogPost bp WHERE bp.id = :postId")
    Optional<BlogPost> findByIdForUpdate(@Param("postId") UUID postId);

    @Query("SELECT COUNT(bp) FROM BlogPost bp WHERE bp.authorId = :authorId AND bp.status = :status")
    long countByAuthorIdAndStatus(@Param("authorId") UUID authorId, @Param("status") BlogStatus status);

    long countByAuthorIdAndCreatedAtAfter(UUID authorId, OffsetDateTime createdAt);

    List<BlogPost> findByNormalizedHashIsNullAndCreatedAtAfter(OffsetDateTime createdAt);

    boolean existsByAuthorIdAndNormalizedHashAndCreatedAtAfter(
            UUID authorId, String normalizedHash, OffsetDateTime createdAt);

    boolean existsByAuthorIdAndNormalizedHashAndCreatedAtAfterAndIdNot(
            UUID authorId, String normalizedHash, OffsetDateTime createdAt, UUID excludedPostId);
}
