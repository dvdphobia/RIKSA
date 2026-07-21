package com.riksa.social.repository;

import com.riksa.social.entity.Reaction;
import com.riksa.social.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

    Optional<Reaction> findByUserIdAndPostIdAndCommentId(UUID userId, UUID postId, UUID commentId);

    boolean existsByUserIdAndPostIdAndType(UUID userId, UUID postId, ReactionType type);

    @Query("SELECT COUNT(r) FROM Reaction r WHERE r.postId IN (SELECT p.id FROM Post p WHERE p.authorId = :authorId)")
    long countReactionsOnUserPosts(@Param("authorId") UUID authorId);
}