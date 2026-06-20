package com.riksa.gamification.repository;

import com.riksa.gamification.entity.RankTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RankTierRepository extends JpaRepository<RankTier, java.util.UUID> {

    /**
     * Find the highest tier the user qualifies for based on their total XP.
     * Returns the tier with the largest minXp that is <= the given XP total.
     */
    Optional<RankTier> findFirstByMinXpLessThanEqualOrderByMinXpDesc(long minXp);
}