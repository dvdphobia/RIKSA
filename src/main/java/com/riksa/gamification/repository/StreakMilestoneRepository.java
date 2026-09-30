package com.riksa.gamification.repository;

import com.riksa.gamification.entity.StreakMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface StreakMilestoneRepository extends JpaRepository<StreakMilestone, java.util.UUID> {

    List<StreakMilestone> findAllByOrderByMinStreakAsc();

    boolean existsByMinStreakAndIdNot(long minStreak, UUID id);

    boolean existsByMinStreak(long minStreak);

    /**
     * Find the highest milestone the user qualifies for based on their streak count.
     * Returns the milestone with the largest minStreak that is <= the given streak.
     */
    Optional<StreakMilestone> findFirstByMinStreakLessThanEqualOrderByMinStreakDesc(long minStreak);
}
