package com.riksa.gamification.repository;

import com.riksa.gamification.entity.XpBoost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface XpBoostRepository extends JpaRepository<XpBoost, UUID> {

    List<XpBoost> findAllByOrderByStartsAtAsc();

    boolean existsByStartsAtBeforeAndEndsAtAfter(OffsetDateTime endsAt, OffsetDateTime startsAt);

    Optional<XpBoost> findFirstByStartsAtLessThanEqualAndEndsAtGreaterThanOrderByStartsAtDesc(
            OffsetDateTime startsAt, OffsetDateTime endsAt);
}
