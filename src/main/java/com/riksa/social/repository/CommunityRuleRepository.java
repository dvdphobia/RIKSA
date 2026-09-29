package com.riksa.social.repository;

import com.riksa.social.entity.CommunityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommunityRuleRepository extends JpaRepository<CommunityRule, UUID> {
    List<CommunityRule> findByCategoryIdOrderByPositionAsc(UUID categoryId);
    void deleteByCategoryId(UUID categoryId);
}
