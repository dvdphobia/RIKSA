package com.riksa.social.repository;

import com.riksa.social.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(UUID postId, Pageable pageable);
    List<Comment> findByParentIdOrderByCreatedAtAsc(UUID parentId, Pageable pageable);
    List<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId);

    int countByPostId(UUID postId);
}
