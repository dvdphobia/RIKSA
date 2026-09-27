package com.riksa.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "buddy_equipped")
@Getter
@Setter
@NoArgsConstructor
public class BuddyEquipped {

    @EmbeddedId
    private BuddyEquippedId id;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;
}
