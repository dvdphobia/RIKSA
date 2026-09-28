package com.riksa.admin.service;

import com.riksa.admin.dto.AdminMarketItemRequest;
import com.riksa.common.audit.AuditLogService;
import com.riksa.gamification.entity.RankTier;
import com.riksa.gamification.repository.RankTierRepository;
import com.riksa.market.entity.MarketItem;
import com.riksa.market.entity.MarketRarity;
import com.riksa.market.entity.MarketSlot;
import com.riksa.market.repository.BuddyEquippedRepository;
import com.riksa.market.repository.BuddyInventoryRepository;
import com.riksa.market.repository.MarketItemRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketItemAdminServiceTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Mock private MarketItemRepository itemRepository;
    @Mock private BuddyInventoryRepository inventoryRepository;
    @Mock private BuddyEquippedRepository equippedRepository;
    @Mock private RankTierRepository rankTierRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private MarketItemAdminService service;

    private final UUID actorId = UUID.randomUUID();

    @Test
    void requestValidationRejectsInvalidPriceAssetKeySpeciesMaskAndWindow() {
        AdminMarketItemRequest request = request();
        request.setPrice(-1L);
        request.setAssetKey("Bad-Key");
        request.setSpeciesMask("crocodile,,unknown");
        request.setAvailableFrom(OffsetDateTime.parse("2026-09-20T00:00:00Z"));
        request.setAvailableUntil(OffsetDateTime.parse("2026-09-19T00:00:00Z"));

        Set<String> fields = VALIDATOR.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(fields).contains("price", "assetKey", "speciesMask", "availabilityWindowValid");
    }

    @Test
    void createPersistsItemAndAuditsCreation() {
        UUID id = UUID.randomUUID();
        when(itemRepository.existsByAssetKey("hat_woven")).thenReturn(false);
        when(itemRepository.saveAndFlush(any(MarketItem.class))).thenAnswer(invocation -> {
            MarketItem item = invocation.getArgument(0);
            item.setId(id);
            return item;
        });

        var response = service.create(actorId, request());

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.active()).isTrue();
        assertThat(response.price()).isEqualTo(50L);
        assertThat(response.ownedCount()).isZero();
        verify(auditLogService).record(actorId, "MARKET_ITEM_CREATE", "MARKET_ITEM", id,
                Map.of("assetKey", "hat_woven", "slot", "HAT"));
    }

    @Test
    void createRejectsDuplicateAssetKeyWithConflictException() {
        when(itemRepository.existsByAssetKey("hat_woven")).thenReturn(true);

        assertThatThrownBy(() -> service.create(actorId, request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("assetKey already exists");
        verify(itemRepository, never()).saveAndFlush(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void createRejectsUnknownRankTierAndStoresCanonicalExistingTier() {
        AdminMarketItemRequest invalid = request();
        invalid.setMinTier("MYTHIC");
        when(itemRepository.existsByAssetKey("hat_woven")).thenReturn(false);
        when(rankTierRepository.findAll()).thenReturn(List.of(rankTier("GOLD")));
        assertThatThrownBy(() -> service.create(actorId, invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minTier");
        verify(itemRepository, never()).saveAndFlush(any());

        AdminMarketItemRequest valid = request();
        valid.setMinTier("gold");
        when(itemRepository.saveAndFlush(any(MarketItem.class))).thenAnswer(invocation -> {
            MarketItem item = invocation.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });
        var response = service.create(actorId, valid);
        assertThat(response.minTier()).isEqualTo("GOLD");
    }

    @Test
    void updateAppliesEveryEditableFieldAndAuditsBeforeAndAfter() {
        UUID id = UUID.randomUUID();
        MarketItem existing = existingItem(id);
        AdminMarketItemRequest update = request();
        update.setSlot(MarketSlot.EYEWEAR);
        update.setName("Updated glasses");
        update.setNameKm("វ៉ែនតា");
        update.setPrice(125L);
        update.setRarity(MarketRarity.EPIC);
        update.setAssetKey("eye_glass");
        update.setSpeciesMask("capybara");
        update.setMinTier(null);
        update.setAvailableFrom(OffsetDateTime.parse("2026-10-01T00:00:00Z"));
        update.setAvailableUntil(OffsetDateTime.parse("2026-11-01T00:00:00Z"));
        update.setSortOrder(7);
        update.setActive(false);
        when(itemRepository.findById(id)).thenReturn(Optional.of(existing));
        when(itemRepository.existsByAssetKeyAndIdNot("eye_glass", id)).thenReturn(false);
        when(itemRepository.saveAndFlush(any(MarketItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.countByIdItemId(id)).thenReturn(4L);
        when(equippedRepository.countByItemId(id)).thenReturn(2L);

        var response = service.update(actorId, id, update);

        assertThat(response.slot()).isEqualTo(MarketSlot.EYEWEAR);
        assertThat(response.name()).isEqualTo("Updated glasses");
        assertThat(response.nameKm()).isEqualTo("វ៉ែនតា");
        assertThat(response.price()).isEqualTo(125L);
        assertThat(response.rarity()).isEqualTo(MarketRarity.EPIC);
        assertThat(response.assetKey()).isEqualTo("eye_glass");
        assertThat(response.speciesMask()).isEqualTo("capybara");
        assertThat(response.minTier()).isNull();
        assertThat(response.availableFrom()).isEqualTo(update.getAvailableFrom());
        assertThat(response.availableUntil()).isEqualTo(update.getAvailableUntil());
        assertThat(response.sortOrder()).isEqualTo(7);
        assertThat(response.active()).isFalse();
        assertThat(response.ownedCount()).isEqualTo(4L);
        assertThat(response.equippedCount()).isEqualTo(2L);
        verify(auditLogService).record(eq(actorId), eq("MARKET_ITEM_UPDATE"), eq("MARKET_ITEM"), eq(id), any(Map.class));
    }

    @Test
    void deleteDeactivatesOwnedItemAndReturnsIt() {
        UUID id = UUID.randomUUID();
        MarketItem item = existingItem(id);
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(inventoryRepository.countByIdItemId(id)).thenReturn(3L);
        when(equippedRepository.countByItemId(id)).thenReturn(1L);
        when(itemRepository.saveAndFlush(item)).thenReturn(item);

        MarketItemAdminService.DeleteResult result = service.delete(actorId, id);

        assertThat(result.deactivated()).isTrue();
        assertThat(result.item().active()).isFalse();
        assertThat(result.item().ownedCount()).isEqualTo(3L);
        verify(itemRepository).saveAndFlush(item);
        verify(itemRepository, never()).delete(any(MarketItem.class));
        verify(auditLogService).record(actorId, "MARKET_ITEM_DEACTIVATE", "MARKET_ITEM", id,
                Map.of("ownedCount", 3L, "assetKey", "hat_woven"));
    }

    @Test
    void deleteHardDeletesUnownedItemAndAudits() {
        UUID id = UUID.randomUUID();
        MarketItem item = existingItem(id);
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(inventoryRepository.countByIdItemId(id)).thenReturn(0L);

        MarketItemAdminService.DeleteResult result = service.delete(actorId, id);

        assertThat(result.deactivated()).isFalse();
        assertThat(result.item()).isNull();
        verify(itemRepository).delete(item);
        verify(auditLogService).record(actorId, "MARKET_ITEM_DELETE", "MARKET_ITEM", id,
                Map.of("assetKey", "hat_woven", "ownedCount", 0));
    }

    private static AdminMarketItemRequest request() {
        AdminMarketItemRequest request = new AdminMarketItemRequest();
        request.setSlot(MarketSlot.HAT);
        request.setName("Woven hat");
        request.setNameKm("មួកត្បាញ");
        request.setPrice(50L);
        request.setRarity(MarketRarity.COMMON);
        request.setAssetKey("hat_woven");
        request.setSortOrder(10);
        request.setActive(true);
        return request;
    }

    private static MarketItem existingItem(UUID id) {
        MarketItem item = new MarketItem();
        item.setId(id);
        item.setSlot(MarketSlot.HAT);
        item.setName("Woven hat");
        item.setNameKm("មួកត្បាញ");
        item.setPrice(50L);
        item.setRarity(MarketRarity.COMMON);
        item.setAssetKey("hat_woven");
        item.setSortOrder(10);
        item.setActive(true);
        return item;
    }

    private static RankTier rankTier(String name) {
        RankTier tier = new RankTier();
        tier.setTierName(com.riksa.common.enums.TierType.valueOf(name));
        return tier;
    }
}
