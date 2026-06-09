package com.riksa.social.repository;

import com.riksa.social.entity.UserCategoryMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserCategoryMembershipRepository extends JpaRepository<UserCategoryMembership, UUID> {

    boolean existsByUserIdAndCategoryId(UUID userId, UUID categoryId);
    void deleteByUserIdAndCategoryId(UUID userId, UUID categoryId);
    int countByCategoryId(UUID categoryId);
    List<UserCategoryMembership> findByUserId(UUID userId);
}