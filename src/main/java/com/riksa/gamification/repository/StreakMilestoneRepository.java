package com.riksa.gamification.repository;

import com.riksa.gamification.entity.StreakMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StreakMilestoneRepository extends JpaRepository<StreakMilestone, UUID> {

    Optional<StreakMilestone> findFirstByMinStreakLessThanEqualOrderByMinStreakDesc(int streak);
}
