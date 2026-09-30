package com.riksa.social.repository;

import com.riksa.social.entity.UserCategoryMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserCategoryMembershipRepository extends JpaRepository<UserCategoryMembership, UUID> {

    boolean existsByUserIdAndCategoryId(UUID userId, UUID categoryId);
    void deleteByUserIdAndCategoryId(UUID userId, UUID categoryId);
    int countByCategoryId(UUID categoryId);
    List<UserCategoryMembership> findByUserId(UUID userId);
    Optional<UserCategoryMembership> findByUserIdAndCategoryId(UUID userId, UUID categoryId);

    /** Owner first, then moderators, then members by join date; {@code pattern} is a lower-case LIKE pattern. */
    @Query("""
        SELECT m FROM UserCategoryMembership m, User u
        WHERE u.id = m.userId
          AND m.categoryId = :categoryId
          AND (lower(u.username) LIKE :pattern OR lower(u.displayName) LIKE :pattern)
        ORDER BY m.role DESC, m.joinedAt ASC
    """)
    List<UserCategoryMembership> searchMembers(@Param("categoryId") UUID categoryId,
                                               @Param("pattern") String pattern,
                                               Pageable pageable);
}