package com.riksa.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuddyInventoryId implements Serializable {
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "item_id")
    private UUID itemId;
}
