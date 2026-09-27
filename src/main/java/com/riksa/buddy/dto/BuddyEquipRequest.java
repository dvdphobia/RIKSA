package com.riksa.buddy.dto;

import com.riksa.market.entity.MarketSlot;

import java.util.UUID;

public record BuddyEquipRequest(MarketSlot slot, UUID itemId) {}
