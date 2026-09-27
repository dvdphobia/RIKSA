package com.riksa.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "buddy_inventory")
@Getter
@Setter
@NoArgsConstructor
public class BuddyInventory {

    @EmbeddedId
    private BuddyInventoryId id;

    @Column(name = "acquired_at", nullable = false)
    private OffsetDateTime acquiredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InventorySource source;

    @Column(name = "price_paid", nullable = false)
    private long pricePaid;

    @Column(name = "idempotency_key")
    private UUID idempotencyKey;
}
