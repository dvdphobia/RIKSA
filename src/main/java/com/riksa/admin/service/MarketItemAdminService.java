package com.riksa.admin.service;

import com.riksa.admin.dto.AdminMarketItem;
import com.riksa.admin.dto.AdminMarketItemRequest;
import com.riksa.common.audit.AuditLogService;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.gamification.entity.RankTier;
import com.riksa.gamification.repository.RankTierRepository;
import com.riksa.market.entity.MarketItem;
import com.riksa.market.entity.MarketSlot;
import com.riksa.market.repository.BuddyEquippedRepository;
import com.riksa.market.repository.BuddyInventoryRepository;
import com.riksa.market.repository.MarketItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketItemAdminService {

    private final MarketItemRepository itemRepository;
    private final BuddyInventoryRepository inventoryRepository;
    private final BuddyEquippedRepository equippedRepository;
    private final RankTierRepository rankTierRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<AdminMarketItem> list(MarketSlot slot, Boolean active) {
        return itemRepository.findAllForAdmin(slot, active).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminMarketItem create(UUID actorId, AdminMarketItemRequest request) {
        validateReferences(request, null);
        MarketItem item = new MarketItem();
        apply(item, request);
        item = save(item);

        auditLogService.record(actorId, "MARKET_ITEM_CREATE", "MARKET_ITEM", item.getId(),
                Map.of("assetKey", item.getAssetKey(), "slot", item.getSlot().name()));
        return toResponse(item, 0, 0);
    }

    @Transactional
    public AdminMarketItem update(UUID actorId, UUID id, AdminMarketItemRequest request) {
        MarketItem item = itemRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Market item not found: " + id));
        validateReferences(request, id);
        if (request.getSlot() != item.getSlot() && inventoryRepository.countByIdItemId(id) > 0) {
            // Equipped rows store their slot in the key, so moving an owned item would
            // leave it worn in its old slot.
            throw new IllegalStateException("Can't change the slot of an item that users already own");
        }
        Map<String, Object> before = snapshot(item);
        apply(item, request);
        item = save(item);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("before", before);
        details.put("after", snapshot(item));
        auditLogService.record(actorId, "MARKET_ITEM_UPDATE", "MARKET_ITEM", item.getId(), details);
        return toResponse(item, inventoryRepository.countByIdItemId(id), equippedRepository.countByItemId(id));
    }

    @Transactional
    public DeleteResult delete(UUID actorId, UUID id) {
        MarketItem item = itemRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Market item not found: " + id));
        long ownedCount = inventoryRepository.countByIdItemId(id);
        if (ownedCount > 0) {
            item.setActive(false);
            item = save(item);
            auditLogService.record(actorId, "MARKET_ITEM_DEACTIVATE", "MARKET_ITEM", id,
                    Map.of("ownedCount", ownedCount, "assetKey", item.getAssetKey()));
            return new DeleteResult(true,
                    toResponse(item, ownedCount, equippedRepository.countByItemId(id)));
        }

        String assetKey = item.getAssetKey();
        itemRepository.delete(item);
        auditLogService.record(actorId, "MARKET_ITEM_DELETE", "MARKET_ITEM", id,
                Map.of("assetKey", assetKey, "ownedCount", 0));
        return new DeleteResult(false, null);
    }

    private void validateReferences(AdminMarketItemRequest request, UUID itemId) {
        boolean assetKeyTaken = itemId == null
                ? itemRepository.existsByAssetKey(request.getAssetKey())
                : itemRepository.existsByAssetKeyAndIdNot(request.getAssetKey(), itemId);
        if (assetKeyTaken) {
            throw new IllegalStateException("Market item assetKey already exists: " + request.getAssetKey());
        }

        if (request.getMinTier() != null) {
            String requestedTier = request.getMinTier().trim();
            if (requestedTier.isEmpty()) {
                throw new IllegalArgumentException("minTier must be an existing rank tier name or null");
            }
            RankTier tier = rankTierRepository.findAll().stream()
                    .filter(candidate -> candidate.getTierName().name().equalsIgnoreCase(requestedTier))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "minTier must be an existing rank tier name or null"));
            request.setMinTier(tier.getTierName().name());
        }
    }

    private MarketItem save(MarketItem item) {
        try {
            return itemRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException("Market item assetKey already exists: " + item.getAssetKey(), exception);
        }
    }

    private void apply(MarketItem item, AdminMarketItemRequest request) {
        item.setSlot(request.getSlot());
        item.setName(request.getName().trim());
        item.setNameKm(request.getNameKm().trim());
        item.setPrice(request.getPrice());
        item.setRarity(request.getRarity());
        item.setAssetKey(request.getAssetKey());
        item.setSpeciesMask(request.getSpeciesMask());
        item.setMinTier(request.getMinTier() == null ? null : request.getMinTier().toUpperCase(Locale.ROOT));
        item.setAvailableFrom(request.getAvailableFrom());
        item.setAvailableUntil(request.getAvailableUntil());
        item.setSortOrder(request.getSortOrder());
        item.setActive(request.getActive());
    }

    private AdminMarketItem toResponse(MarketItemRepository.MarketItemAdminProjection projection) {
        return new AdminMarketItem(
                projection.getId(), projection.getSlot(), projection.getName(), projection.getNameKm(),
                projection.getPrice(), projection.getRarity(), projection.getAssetKey(), projection.getSpeciesMask(),
                projection.getMinTier(), projection.getAvailableFrom(), projection.getAvailableUntil(),
                projection.getSortOrder(), projection.getActive(), projection.getOwnedCount(),
                projection.getEquippedCount());
    }

    private AdminMarketItem toResponse(MarketItem item, long ownedCount, long equippedCount) {
        return new AdminMarketItem(
                item.getId(), item.getSlot(), item.getName(), item.getNameKm(), item.getPrice(), item.getRarity(),
                item.getAssetKey(), item.getSpeciesMask(), item.getMinTier(), item.getAvailableFrom(),
                item.getAvailableUntil(), item.getSortOrder(), item.isActive(), ownedCount, equippedCount);
    }

    private Map<String, Object> snapshot(MarketItem item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("slot", item.getSlot().name());
        result.put("name", item.getName());
        result.put("nameKm", item.getNameKm());
        result.put("price", item.getPrice());
        result.put("rarity", item.getRarity().name());
        result.put("assetKey", item.getAssetKey());
        result.put("speciesMask", item.getSpeciesMask());
        result.put("minTier", item.getMinTier());
        result.put("availableFrom", item.getAvailableFrom() == null ? null : item.getAvailableFrom().toString());
        result.put("availableUntil", item.getAvailableUntil() == null ? null : item.getAvailableUntil().toString());
        result.put("sortOrder", item.getSortOrder());
        result.put("active", item.isActive());
        return result;
    }

    public record DeleteResult(boolean deactivated, AdminMarketItem item) {}
}
