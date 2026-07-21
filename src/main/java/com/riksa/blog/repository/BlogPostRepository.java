package com.riksa.blog.repository;

import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {
    Page<BlogPost> findByStatus(BlogStatus status, Pageable pageable);
    Page<BlogPost> findByStatusAndCategoryId(BlogStatus status, UUID categoryId, Pageable pageable);
    Page<BlogPost> findByAuthorId(UUID authorId, Pageable pageable);

    @Query("SELECT COUNT(bp) FROM BlogPost bp WHERE bp.authorId = :authorId AND bp.status = :status")
    long countByAuthorIdAndStatus(@Param("authorId") UUID authorId, @Param("status") BlogStatus status);
}
