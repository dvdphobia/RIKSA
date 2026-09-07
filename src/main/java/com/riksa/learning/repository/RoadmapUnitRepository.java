package com.riksa.learning.repository;

import com.riksa.learning.entity.RoadmapUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoadmapUnitRepository extends JpaRepository<RoadmapUnit, UUID> {
    List<RoadmapUnit> findByCategoryIdOrderByPositionAsc(UUID categoryId);
}
