package com.riksa.market.dto;

import com.riksa.market.entity.MarketRarity;
import com.riksa.market.entity.MarketSlot;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MarketItemResponse(
        UUID id,
        MarketSlot slot,
        String name,
        String nameKm,
        long price,
        MarketRarity rarity,
        String assetKey,
        boolean owned,
        boolean equipped,
        boolean locked,
        String lockReason,
        OffsetDateTime availableUntil) {}
