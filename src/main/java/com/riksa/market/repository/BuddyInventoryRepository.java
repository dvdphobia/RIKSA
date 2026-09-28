package com.riksa.market.repository;

import com.riksa.market.entity.BuddyInventory;
import com.riksa.market.entity.BuddyInventoryId;
import com.riksa.market.entity.InventorySource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuddyInventoryRepository extends JpaRepository<BuddyInventory, BuddyInventoryId> {
    List<BuddyInventory> findByIdUserId(UUID userId);

    long countByIdItemId(UUID itemId);

    Optional<BuddyInventory> findByIdUserIdAndIdempotencyKey(UUID userId, UUID idempotencyKey);

    boolean existsByIdUserIdAndIdItemId(UUID userId, UUID itemId);

    Optional<BuddyInventory> findFirstByIdUserIdAndSourceAndAcquiredAtGreaterThanEqualAndAcquiredAtLessThanOrderByAcquiredAtDesc(
            UUID userId, InventorySource source, OffsetDateTime from, OffsetDateTime until);
}
