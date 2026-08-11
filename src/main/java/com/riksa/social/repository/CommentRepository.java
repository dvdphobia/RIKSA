package com.riksa.social.repository;

import com.riksa.social.entity.Comment;
import com.riksa.social.enums.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPostIdAndParentIdIsNullAndStatusOrderByCreatedAtAsc(UUID postId, ContentStatus status, Pageable pageable);
    List<Comment> findByParentIdAndStatusOrderByCreatedAtAsc(UUID parentId, ContentStatus status, Pageable pageable);
    List<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId);

    Page<Comment> findByStatus(ContentStatus status, Pageable pageable);

    int countByPostId(UUID postId);

    long countByCreatedAtAfter(java.time.OffsetDateTime cutoff);
}
