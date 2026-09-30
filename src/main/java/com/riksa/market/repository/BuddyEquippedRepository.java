package com.riksa.market.repository;

import com.riksa.market.entity.BuddyEquipped;
import com.riksa.market.entity.BuddyEquippedId;
import com.riksa.market.entity.MarketSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BuddyEquippedRepository extends JpaRepository<BuddyEquipped, BuddyEquippedId> {
    List<BuddyEquipped> findByIdUserId(UUID userId);

    long countByItemId(UUID itemId);

    @Modifying
    @Query("delete from BuddyEquipped equipped where equipped.id.userId = :userId and equipped.id.slot = :slot")
    void deleteByUserAndSlot(@Param("userId") UUID userId, @Param("slot") MarketSlot slot);

    @Query("""
            select equipped.id.slot as slot, equipped.itemId as itemId, item.assetKey as assetKey
            from BuddyEquipped equipped join MarketItem item on item.id = equipped.itemId
            where equipped.id.userId = :userId
            """)
    List<EquippedItemView> findDetailsByUserId(@Param("userId") UUID userId);

    interface EquippedItemView {
        MarketSlot getSlot();
        UUID getItemId();
        String getAssetKey();
    }
}
