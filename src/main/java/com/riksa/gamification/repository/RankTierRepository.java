package com.riksa.gamification.repository;

import com.riksa.gamification.entity.RankTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RankTierRepository extends JpaRepository<RankTier, UUID> {

    Optional<RankTier> findFirstByMinXpLessThanEqualOrderByMinXpDesc(int xp);
}
