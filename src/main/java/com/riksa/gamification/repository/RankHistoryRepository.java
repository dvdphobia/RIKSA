package com.riksa.gamification.repository;

import com.riksa.gamification.entity.RankHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RankHistoryRepository extends JpaRepository<RankHistory, UUID> {
}
