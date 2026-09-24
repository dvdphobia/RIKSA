package com.riksa.buddy.repository;

import com.riksa.buddy.entity.BuddyCareEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface BuddyCareEventRepository extends JpaRepository<BuddyCareEvent, UUID> {

    boolean existsByUserIdAndSourceKeyAndSourceId(UUID userId, String sourceKey, UUID sourceId);

    long countByUserIdAndSourceKeyAndCareDay(UUID userId, String sourceKey, LocalDate careDay);
}
