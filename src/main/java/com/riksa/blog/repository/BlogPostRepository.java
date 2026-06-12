package com.riksa.blog.repository;

import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {
    Page<BlogPost> findByStatus(BlogStatus status, Pageable pageable);
    Page<BlogPost> findByStatusAndCategoryId(BlogStatus status, UUID categoryId, Pageable pageable);
    Page<BlogPost> findByAuthorId(UUID authorId, Pageable pageable);
}
