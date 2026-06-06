package com.riksa.gamification.repository;

import com.riksa.gamification.entity.UserGamification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGamificationRepository extends JpaRepository<UserGamification, UUID> {

    Optional<UserGamification> findByUserId(UUID userId);

    // leaderboard queries
    // List<UserGamification> findTop10ByOrderByTotalXpDesc();
    List<UserGamification> findAllByOrderByTotalXpDesc(Pageable pageable);

    // Active streak users
    List<UserGamification> findByCurrentStreakGreaterThan(int streak);

    // Users who haven't completed anything since a given time (for streak reset jobs)
    List<UserGamification> findByLastCompletedAtBefore(OffsetDateTime cutoff);

    // Check if a user exists
    boolean existsByUserId(UUID userID);

}

