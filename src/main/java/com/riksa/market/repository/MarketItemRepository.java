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
}
