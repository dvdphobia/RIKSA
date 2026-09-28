package com.riksa.market.repository;

import com.riksa.market.entity.MarketItem;
import com.riksa.market.entity.MarketRarity;
import com.riksa.market.entity.MarketSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface MarketItemRepository extends JpaRepository<MarketItem, UUID> {

    boolean existsByAssetKey(String assetKey);

    boolean existsByAssetKeyAndIdNot(String assetKey, UUID id);

    @Query("""
            select item.id as id,
                   item.slot as slot,
                   item.name as name,
                   item.nameKm as nameKm,
                   item.price as price,
                   item.rarity as rarity,
                   item.assetKey as assetKey,
                   item.speciesMask as speciesMask,
                   item.minTier as minTier,
                   item.availableFrom as availableFrom,
                   item.availableUntil as availableUntil,
                   item.sortOrder as sortOrder,
                   item.active as active,
                   (select count(inventory) from BuddyInventory inventory
                    where inventory.id.itemId = item.id) as ownedCount,
                   (select count(equipped) from BuddyEquipped equipped
                    where equipped.itemId = item.id) as equippedCount
            from MarketItem item
            where (:slot is null or item.slot = :slot)
              and (:active is null or item.active = :active)
            order by item.slot asc, item.sortOrder asc, item.id asc
            """)
    List<MarketItemAdminProjection> findAllForAdmin(@Param("slot") MarketSlot slot,
                                                     @Param("active") Boolean active);

    @Query("""
            select item from MarketItem item
            where item.active = true
              and (item.availableFrom is null or item.availableFrom <= :now)
              and (item.availableUntil is null or item.availableUntil > :now)
              and (:slot is null or item.slot = :slot)
            order by item.sortOrder asc, item.price asc
            """)
    List<MarketItem> findAvailableItems(@Param("slot") MarketSlot slot,
                                        @Param("now") OffsetDateTime now);

    @Query("""
            select item from MarketItem item
            where item.active = true and item.rarity = :rarity
              and (item.availableFrom is null or item.availableFrom <= :now)
              and (item.availableUntil is null or item.availableUntil > :now)
            order by item.sortOrder asc, item.price asc
            """)
    List<MarketItem> findAvailableByRarity(@Param("rarity") MarketRarity rarity,
                                           @Param("now") OffsetDateTime now);

    interface MarketItemAdminProjection {
        UUID getId();
        MarketSlot getSlot();
        String getName();
        String getNameKm();
        long getPrice();
        MarketRarity getRarity();
        String getAssetKey();
        String getSpeciesMask();
        String getMinTier();
        OffsetDateTime getAvailableFrom();
        OffsetDateTime getAvailableUntil();
        int getSortOrder();
        boolean getActive();
        long getOwnedCount();
        long getEquippedCount();
    }
}
