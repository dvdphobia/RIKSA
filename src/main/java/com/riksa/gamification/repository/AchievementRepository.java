package com.riksa.gamification.repository;

import com.riksa.gamification.entity.Achievement;
import com.riksa.common.enums.AchievementCriteriaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, UUID> {

    List<Achievement> findByCriteriaType(AchievementCriteriaType criteriaType);
}
