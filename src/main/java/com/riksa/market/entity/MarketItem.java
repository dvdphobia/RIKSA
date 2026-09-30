package com.riksa.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "market_item")
@Getter
@Setter
@NoArgsConstructor
public class MarketItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MarketSlot slot;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "name_km", nullable = false, length = 120)
    private String nameKm;

    @Column(nullable = false)
    private long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MarketRarity rarity;

    @Column(name = "asset_key", nullable = false, unique = true, length = 64)
    private String assetKey;

    @Column(name = "species_mask")
    private String speciesMask;

    @Column(name = "min_tier", length = 32)
    private String minTier;

    @Column(name = "available_from")
    private OffsetDateTime availableFrom;

    @Column(name = "available_until")
    private OffsetDateTime availableUntil;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;
}
