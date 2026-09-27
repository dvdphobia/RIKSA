package com.riksa.buddy.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BuddyPublicLookRepository extends Repository<com.riksa.buddy.entity.Buddy, UUID> {

    @Query(value = """
            select b.user_id as "userId", b.species as species, b.name as name,
                   equipped.slot as slot, item.asset_key as "assetKey"
            from buddy b
            left join buddy_equipped equipped on equipped.user_id = b.user_id
            left join market_item item on item.id = equipped.item_id
            where b.user_id in (:userIds)
            order by b.user_id, equipped.slot
            """, nativeQuery = true)
    List<BuddyPublicLookRow> findLooksByUserIds(@Param("userIds") Collection<UUID> userIds);

    interface BuddyPublicLookRow {
        UUID getUserId();
        String getSpecies();
        String getName();
        String getSlot();
        String getAssetKey();
    }
}
