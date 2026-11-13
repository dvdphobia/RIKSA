package com.riksa.social.repository;

import com.riksa.social.entity.Comment;
import com.riksa.social.enums.ContentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Comment c WHERE c.id = :commentId")
    Optional<Comment> findByIdForUpdate(@Param("commentId") UUID commentId);

    List<Comment> findByPostIdAndParentIdIsNullAndStatusOrderByCreatedAtAscIdAsc(
            UUID postId, ContentStatus status, Pageable pageable);

    @Query("""
            SELECT c FROM Comment c
            WHERE c.postId = :postId
              AND c.parentId IS NULL
              AND c.status = :status
              AND (c.createdAt > :cursorCreatedAt
                   OR (c.createdAt = :cursorCreatedAt AND c.id > :cursorId))
            ORDER BY c.createdAt ASC, c.id ASC
            """)
    List<Comment> findPostCommentsAfterCursor(
            @Param("postId") UUID postId,
            @Param("status") ContentStatus status,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);

    List<Comment> findByParentIdAndStatusOrderByCreatedAtAscIdAsc(
            UUID parentId, ContentStatus status, Pageable pageable);

    @Query("""
            SELECT c FROM Comment c
            WHERE c.parentId = :parentId
              AND c.status = :status
              AND (c.createdAt > :cursorCreatedAt
                   OR (c.createdAt = :cursorCreatedAt AND c.id > :cursorId))
            ORDER BY c.createdAt ASC, c.id ASC
            """)
    List<Comment> findRepliesAfterCursor(
            @Param("parentId") UUID parentId,
            @Param("status") ContentStatus status,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);
    List<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId);

    List<Comment> findByPostIdIn(List<UUID> postIds);

    List<Comment> findByAuthorId(UUID authorId);

    Page<Comment> findByStatus(ContentStatus status, Pageable pageable);

    int countByPostId(UUID postId);

    long countByCreatedAtAfter(java.time.OffsetDateTime cutoff);

    long countByAuthorIdAndCreatedAtAfter(UUID authorId, OffsetDateTime cutoff);

    List<Comment> findByAuthorIdAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
            UUID authorId, OffsetDateTime cutoff, Pageable pageable);
}
