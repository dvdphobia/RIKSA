package com.riksa.social.repository;

import com.riksa.social.entity.Share;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShareRepository extends JpaRepository<Share, UUID> {
    int countByPostId(UUID postId);
}