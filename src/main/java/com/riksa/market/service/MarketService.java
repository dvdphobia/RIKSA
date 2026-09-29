package com.riksa.market.service;

import com.riksa.buddy.entity.Buddy;
import com.riksa.buddy.repository.BuddyRepository;
import com.riksa.common.cache.CacheNames;
import com.riksa.gamification.entity.RankTier;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.repository.RankTierRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.market.dto.ChestRewardItemResponse;
import com.riksa.market.dto.MarketItemResponse;
import com.riksa.market.dto.MarketItemsResponse;
import com.riksa.market.dto.PurchaseResponse;
import com.riksa.buddy.dto.BuddyEquippedItem;
import com.riksa.market.entity.BuddyEquipped;
import com.riksa.market.entity.BuddyEquippedId;
import com.riksa.market.entity.BuddyInventory;
import com.riksa.market.entity.BuddyInventoryId;
import com.riksa.market.entity.InventorySource;
import com.riksa.market.entity.MarketItem;
import com.riksa.market.entity.MarketRarity;
import com.riksa.market.entity.MarketSlot;
import com.riksa.market.repository.BuddyEquippedRepository;
import com.riksa.market.repository.BuddyInventoryRepository;
import com.riksa.market.repository.MarketItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final MarketItemRepository itemRepository;
    private final BuddyInventoryRepository inventoryRepository;
    private final BuddyEquippedRepository equippedRepository;
    private final UserGamificationRepository userGamificationRepository;
    private final RankTierRepository rankTierRepository;
    private final BuddyRepository buddyRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MarketItemsResponse listItems(UUID userId, MarketSlot slot) {
        UserGamification gamification = requireGamification(userId);
        OffsetDateTime now = now();
        String species = buddyRepository.findById(userId).map(Buddy::getSpecies).orElse("crocodile");
        List<RankTier> tiers = rankTierRepository.findAll();
        Set<UUID> owned = new HashSet<>();
        inventoryRepository.findByIdUserId(userId).forEach(row -> owned.add(row.getId().getItemId()));
        Map<MarketSlot, UUID> equipped = new HashMap<>();
        equippedRepository.findByIdUserId(userId).forEach(row ->
                equipped.put(row.getId().getSlot(), row.getItemId()));

        List<MarketItemResponse> items = itemRepository.findAvailableItems(slot, now).stream()
                .map(item -> itemResponse(item, owned, equipped, species, gamification.getTotalXp(), tiers))
                .toList();
        return new MarketItemsResponse(gamification.getXpBalance(), items);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.GAMIFICATION_PROFILE, key = "#userId.toString()",
            condition = "@redisCacheFeatureSwitch.enabled()")
    public PurchaseResponse purchase(UUID userId, UUID itemId, String idempotencyHeader) {
        UUID idempotencyKey = parseIdempotencyKey(idempotencyHeader);

        Optional<BuddyInventory> previous = inventoryRepository.findByIdUserIdAndIdempotencyKey(
                userId, idempotencyKey);
        if (previous.isPresent()) return replayPurchase(userId, previous.get());

        UserGamification gamification = userGamificationRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new MarketException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND",
                        "Gamification profile not found"));

        // Recheck after acquiring the per-user lock: a concurrent request may have committed this key.
        previous = inventoryRepository.findByIdUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (previous.isPresent()) return replayPurchase(userId, previous.get());

        // Share-lock the item so an admin delete can't remove it (and cascade this
        // inventory row away) between the availability check and the insert.
        MarketItem item = itemRepository.findByIdForShare(itemId).orElse(null);
        OffsetDateTime now = now();
        if (!isAvailable(item, now)) {
            throw new MarketException(HttpStatus.GONE, "NOT_AVAILABLE", "This item is not available");
        }

        String species = buddyRepository.findById(userId).map(Buddy::getSpecies).orElse("crocodile");
        List<RankTier> tiers = rankTierRepository.findAll();
        String lockReason = lockReason(item, species, gamification.getTotalXp(), tiers);
        if (lockReason != null) {
            throw new MarketException(HttpStatus.FORBIDDEN, "LOCKED", "This item is locked");
        }
        if (inventoryRepository.existsByIdUserIdAndIdItemId(userId, itemId)) {
            throw new MarketException(HttpStatus.CONFLICT, "ALREADY_OWNED", "You already own this item");
        }
        if (gamification.getXpBalance() < item.getPrice()) {
            throw new MarketException(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE",
                    "Not enough XP to purchase this item");
        }

        gamification.setXpBalance(gamification.getXpBalance() - item.getPrice());
        userGamificationRepository.save(gamification);

        BuddyInventory inventory = new BuddyInventory();
        inventory.setId(new BuddyInventoryId(userId, itemId));
        inventory.setAcquiredAt(now);
        inventory.setSource(InventorySource.PURCHASE);
        inventory.setPricePaid(item.getPrice());
        inventory.setIdempotencyKey(idempotencyKey);
        inventoryRepository.saveAndFlush(inventory);

        Set<UUID> owned = Set.of(itemId);
        Map<MarketSlot, UUID> equipped = currentEquipped(userId);
        MarketItemResponse responseItem = itemResponse(item, owned, equipped, species,
                gamification.getTotalXp(), tiers);
        return new PurchaseResponse(responseItem, gamification.getXpBalance());
    }

    @Transactional
    public Optional<ChestRewardItemResponse> grantChestItem(UUID userId) {
        List<MarketItem> commons = itemRepository.findAvailableByRarity(MarketRarity.COMMON, now());
        if (commons.isEmpty()) return Optional.empty();

        Set<UUID> owned = new HashSet<>();
        inventoryRepository.findByIdUserId(userId).forEach(row -> owned.add(row.getId().getItemId()));
        List<MarketItem> unowned = commons.stream().filter(item -> !owned.contains(item.getId())).toList();
        if (unowned.isEmpty()) return Optional.empty();

        MarketItem picked = unowned.get(ThreadLocalRandom.current().nextInt(unowned.size()));
        // Same share lock as purchase; skip the reward if an admin just removed the item.
        MarketItem reward = itemRepository.findByIdForShare(picked.getId()).orElse(null);
        if (reward == null || !reward.isActive()) return Optional.empty();
        BuddyInventory inventory = new BuddyInventory();
        inventory.setId(new BuddyInventoryId(userId, reward.getId()));
        inventory.setAcquiredAt(now());
        inventory.setSource(InventorySource.CHEST);
        inventory.setPricePaid(0);
        inventoryRepository.saveAndFlush(inventory);
        return Optional.of(chestReward(reward));
    }

    @Transactional(readOnly = true)
    public Optional<ChestRewardItemResponse> findChestReward(UUID userId,
                                                              OffsetDateTime from,
                                                              OffsetDateTime until) {
        return inventoryRepository
                .findFirstByIdUserIdAndSourceAndAcquiredAtGreaterThanEqualAndAcquiredAtLessThanOrderByAcquiredAtDesc(
                        userId, InventorySource.CHEST, from, until)
                .flatMap(inventory -> itemRepository.findById(inventory.getId().getItemId()))
                .map(this::chestReward);
    }

    @Transactional(readOnly = true)
    public long getBalance(UUID userId) {
        return requireGamification(userId).getXpBalance();
    }

    public Map<MarketSlot, UUID> currentEquipped(UUID userId) {
        Map<MarketSlot, UUID> equipped = new HashMap<>();
        equippedRepository.findByIdUserId(userId).forEach(row ->
                equipped.put(row.getId().getSlot(), row.getItemId()));
        return equipped;
    }

    @Transactional(readOnly = true)
    public Map<String, BuddyEquippedItem> equippedItems(UUID userId) {
        Map<String, BuddyEquippedItem> result = new java.util.LinkedHashMap<>();
        equippedRepository.findDetailsByUserId(userId).forEach(row ->
                result.put(row.getSlot().name(), new BuddyEquippedItem(row.getItemId(), row.getAssetKey())));
        return result;
    }

    @Transactional
    public void equip(UUID userId, MarketSlot slot, UUID itemId) {
        if (slot == null) throw new IllegalArgumentException("A valid buddy equipment slot is required");
        if (itemId == null) {
            equippedRepository.deleteByUserAndSlot(userId, slot);
            return;
        }
        if (!inventoryRepository.existsByIdUserIdAndIdItemId(userId, itemId)) {
            throw new MarketException(HttpStatus.FORBIDDEN, "NOT_OWNED", "You do not own this item");
        }
        MarketItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new MarketException(HttpStatus.FORBIDDEN, "NOT_OWNED", "You do not own this item"));
        if (item.getSlot() != slot) {
            throw new IllegalArgumentException("Item does not match the requested slot");
        }
        BuddyEquipped equipped = new BuddyEquipped();
        equipped.setId(new BuddyEquippedId(userId, slot));
        equipped.setItemId(itemId);
        equippedRepository.save(equipped);
    }

    private PurchaseResponse replayPurchase(UUID userId, BuddyInventory inventory) {
        MarketItem item = itemRepository.findById(inventory.getId().getItemId())
                .orElseThrow(() -> new MarketException(HttpStatus.GONE, "NOT_AVAILABLE", "This item is not available"));
        UserGamification gamification = requireGamification(userId);
        String species = buddyRepository.findById(userId).map(Buddy::getSpecies).orElse("crocodile");
        MarketItemResponse response = itemResponse(item, Set.of(item.getId()), currentEquipped(userId), species,
                gamification.getTotalXp(), rankTierRepository.findAll());
        return new PurchaseResponse(response, gamification.getXpBalance());
    }

    private MarketItemResponse itemResponse(MarketItem item,
                                            Set<UUID> owned,
                                            Map<MarketSlot, UUID> equipped,
                                            String species,
                                            long totalXp,
                                            List<RankTier> tiers) {
        String lockReason = lockReason(item, species, totalXp, tiers);
        return new MarketItemResponse(item.getId(), item.getSlot(), item.getName(), item.getNameKm(),
                item.getPrice(), item.getRarity(), item.getAssetKey(), owned.contains(item.getId()),
                item.getId().equals(equipped.get(item.getSlot())), lockReason != null, lockReason,
                item.getAvailableUntil());
    }

    private String lockReason(MarketItem item, String species, long totalXp, List<RankTier> tiers) {
        if (item.getSpeciesMask() != null && !item.getSpeciesMask().isBlank()) {
            boolean allowed = java.util.Arrays.stream(item.getSpeciesMask().split(","))
                    .map(String::trim).anyMatch(value -> value.equalsIgnoreCase(species));
            if (!allowed) return "SPECIES";
        }
        if (item.getMinTier() != null && !item.getMinTier().isBlank()) {
            RankTier required = tiers.stream().filter(t -> t.getTierName().name().equalsIgnoreCase(item.getMinTier()))
                    .findFirst().orElse(null);
            if (required == null || totalXp < required.getMinXp()) return "TIER";
        }
        return null;
    }

    private boolean isAvailable(MarketItem item, OffsetDateTime now) {
        return item != null && item.isActive()
                && (item.getAvailableFrom() == null || !item.getAvailableFrom().isAfter(now))
                && (item.getAvailableUntil() == null || item.getAvailableUntil().isAfter(now));
    }

    private UserGamification requireGamification(UUID userId) {
        return userGamificationRepository.findByUserId(userId)
                .orElseThrow(() -> new MarketException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND",
                        "Gamification profile not found"));
    }

    private UUID parseIdempotencyKey(String header) {
        if (header == null || header.isBlank()) {
            throw new MarketException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "A valid Idempotency-Key UUID is required");
        }
        try {
            UUID parsed = UUID.fromString(header.trim());
            if (!parsed.toString().equalsIgnoreCase(header.trim())) throw new IllegalArgumentException();
            return parsed;
        } catch (IllegalArgumentException exception) {
            throw new MarketException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "A valid Idempotency-Key UUID is required");
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private ChestRewardItemResponse chestReward(MarketItem item) {
        return new ChestRewardItemResponse(item.getId(), item.getSlot(), item.getAssetKey(),
                item.getName(), item.getNameKm());
    }
}
