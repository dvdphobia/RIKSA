package com.riksa.gamification.repository;

import com.riksa.gamification.entity.StreakMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StreakMilestoneRepository extends JpaRepository<StreakMilestone, java.util.UUID> {

    /**
     * Find the highest milestone the user qualifies for based on their streak count.
     * Returns the milestone with the largest minStreak that is <= the given streak.
     */
    Optional<StreakMilestone> findFirstByMinStreakLessThanEqualOrderByMinStreakDesc(long minStreak);
}