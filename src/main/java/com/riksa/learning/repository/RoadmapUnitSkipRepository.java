package com.riksa.learning.repository;

import com.riksa.learning.entity.RoadmapUnitSkip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoadmapUnitSkipRepository extends JpaRepository<RoadmapUnitSkip, UUID> {
    List<RoadmapUnitSkip> findByUserIdAndRoadmapUnitIdIn(UUID userId, List<UUID> roadmapUnitIds);
    Optional<RoadmapUnitSkip> findByUserIdAndRoadmapUnitId(UUID userId, UUID roadmapUnitId);
    void deleteByUserIdAndRoadmapUnitId(UUID userId, UUID roadmapUnitId);
}
