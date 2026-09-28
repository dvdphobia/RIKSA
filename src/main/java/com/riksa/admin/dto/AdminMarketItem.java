package com.riksa.admin.dto;

import com.riksa.market.entity.MarketRarity;
import com.riksa.market.entity.MarketSlot;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminMarketItem(
        UUID id,
        MarketSlot slot,
        String name,
        String nameKm,
        long price,
        MarketRarity rarity,
        String assetKey,
        String speciesMask,
        String minTier,
        OffsetDateTime availableFrom,
        OffsetDateTime availableUntil,
        int sortOrder,
        boolean active,
        long ownedCount,
        long equippedCount
) {}
