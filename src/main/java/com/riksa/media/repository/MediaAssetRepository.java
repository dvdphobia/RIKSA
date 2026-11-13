package com.riksa.media.repository;

import com.riksa.media.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, String> {
    List<MediaAsset> findAllByOwnerId(UUID ownerId);
}
