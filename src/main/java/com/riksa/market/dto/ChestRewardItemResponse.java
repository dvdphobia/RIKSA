package com.riksa.market.dto;

import com.riksa.market.entity.MarketSlot;

import java.util.UUID;

public record ChestRewardItemResponse(UUID id, MarketSlot slot, String assetKey, String name, String nameKm) {}
