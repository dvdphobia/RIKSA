package com.riksa.blog.repository;

import com.riksa.blog.entity.BlogView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BlogViewRepository extends JpaRepository<BlogView, UUID> {
    int countByBlogPostId(UUID blogPostId);
}
