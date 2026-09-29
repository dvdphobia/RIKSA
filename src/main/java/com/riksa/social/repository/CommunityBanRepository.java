package com.riksa.social.repository;

import com.riksa.social.entity.CommunityBan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommunityBanRepository extends JpaRepository<CommunityBan, CommunityBan.Key> {
    boolean existsByCategoryIdAndUserId(UUID categoryId, UUID userId);
    List<CommunityBan> findByCategoryIdOrderByCreatedAtDesc(UUID categoryId);
    void deleteByCategoryIdAndUserId(UUID categoryId, UUID userId);
}
