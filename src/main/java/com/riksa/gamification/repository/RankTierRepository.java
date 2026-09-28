package com.riksa.gamification.repository;

import com.riksa.gamification.entity.RankTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface RankTierRepository extends JpaRepository<RankTier, java.util.UUID> {

    List<RankTier> findAllByOrderByMinXpAsc();

    /**
     * Find the highest tier the user qualifies for based on their total XP.
     * Returns the tier with the largest minXp that is <= the given XP total.
     */
    Optional<RankTier> findFirstByMinXpLessThanEqualOrderByMinXpDesc(long minXp);

    /**
     * Find the next tier above the given XP total: the smallest minXp still out of reach.
     * Ordering is explicit because {@code findAll()} makes no guarantee about row order.
     */
    Optional<RankTier> findFirstByMinXpGreaterThanOrderByMinXpAsc(long minXp);
}
