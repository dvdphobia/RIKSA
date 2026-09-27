package com.riksa.market.service;

import com.riksa.buddy.entity.Buddy;
import com.riksa.buddy.repository.BuddyRepository;
import com.riksa.common.enums.TierType;
import com.riksa.gamification.entity.RankTier;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.repository.RankTierRepository;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.market.entity.BuddyInventory;
import com.riksa.market.entity.BuddyInventoryId;
import com.riksa.market.entity.InventorySource;
import com.riksa.market.entity.MarketItem;
import com.riksa.market.entity.MarketRarity;
import com.riksa.market.entity.MarketSlot;
import com.riksa.market.repository.BuddyEquippedRepository;
import com.riksa.market.repository.BuddyInventoryRepository;
import com.riksa.market.repository.MarketItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID IDEMPOTENCY_KEY = UUID.randomUUID();
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-24T10:00:00Z");

    @Mock MarketItemRepository itemRepository;
    @Mock BuddyInventoryRepository inventoryRepository;
    @Mock BuddyEquippedRepository equippedRepository;
    @Mock UserGamificationRepository userGamificationRepository;
    @Mock RankTierRepository rankTierRepository;
    @Mock BuddyRepository buddyRepository;

    private MarketService service;
    private UserGamification gamification;
    private MarketItem item;

    @BeforeEach
    void setUp() {
        service = new MarketService(itemRepository, inventoryRepository, equippedRepository,
                userGamificationRepository, rankTierRepository, buddyRepository,
                Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
        gamification = new UserGamification();
        gamification.setUserId(USER_ID);
        gamification.setTotalXp(1_000);
        gamification.setXpBalance(1_000);
        item = item(MarketSlot.HAT, 300);

        lenient().when(userGamificationRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(gamification));
        lenient().when(userGamificationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(gamification));
        lenient().when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        lenient().when(inventoryRepository.existsByIdUserIdAndIdItemId(USER_ID, ITEM_ID)).thenReturn(false);
        lenient().when(inventoryRepository.saveAndFlush(any(BuddyInventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void purchaseDebitsOnlySpendableBalanceAndGrantsOwnership() {
        var response = service.purchase(USER_ID, ITEM_ID, IDEMPOTENCY_KEY.toString());

        assertThat(response.item().owned()).isTrue();
        assertThat(response.balance()).isEqualTo(700);
        assertThat(gamification.getTotalXp()).isEqualTo(1_000);
        assertThat(gamification.getXpBalance()).isEqualTo(700);
        verify(userGamificationRepository).save(gamification);
        verify(inventoryRepository).saveAndFlush(any(BuddyInventory.class));
    }

    @Test
    void insufficientBalanceReturnsConflict() {
        gamification.setXpBalance(299);
        assertMarketError("INSUFFICIENT_BALANCE", HttpStatus.CONFLICT,
                () -> service.purchase(USER_ID, ITEM_ID, IDEMPOTENCY_KEY.toString()));
    }

    @Test
    void alreadyOwnedReturnsConflict() {
        when(inventoryRepository.existsByIdUserIdAndIdItemId(USER_ID, ITEM_ID)).thenReturn(true);
        assertMarketError("ALREADY_OWNED", HttpStatus.CONFLICT,
                () -> service.purchase(USER_ID, ITEM_ID, IDEMPOTENCY_KEY.toString()));
    }

    @Test
    void itemOutsideAvailabilityWindowReturnsGone() {
        item.setAvailableUntil(NOW.minusSeconds(1));
        assertMarketError("NOT_AVAILABLE", HttpStatus.GONE,
                () -> service.purchase(USER_ID, ITEM_ID, IDEMPOTENCY_KEY.toString()));
    }

    @Test
    void speciesRestrictedItemReturnsForbidden() {
        item.setSpeciesMask("capybara");
        Buddy buddy = new Buddy();
        buddy.setSpecies("crocodile");
        when(buddyRepository.findById(USER_ID)).thenReturn(Optional.of(buddy));
        assertMarketError("LOCKED", HttpStatus.FORBIDDEN,
                () -> service.purchase(USER_ID, ITEM_ID, IDEMPOTENCY_KEY.toString()));
    }

    @Test
    void missingOrInvalidIdempotencyKeyReturnsBadRequest() {
        assertMarketError("INVALID_IDEMPOTENCY_KEY", HttpStatus.BAD_REQUEST,
                () -> service.purchase(USER_ID, ITEM_ID, null));
        assertMarketError("INVALID_IDEMPOTENCY_KEY", HttpStatus.BAD_REQUEST,
                () -> service.purchase(USER_ID, ITEM_ID, "not-a-uuid"));
        verify(userGamificationRepository, never()).findByIdForUpdate(USER_ID);
    }

    @Test
    void retryWithSameKeyReturnsExistingPurchaseWithoutChargingAgain() {
        BuddyInventory previous = inventory(ITEM_ID, IDEMPOTENCY_KEY, InventorySource.PURCHASE, 300);
        when(inventoryRepository.findByIdUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(previous));

        var first = service.purchase(USER_ID, ITEM_ID, IDEMPOTENCY_KEY.toString());
        var retry = service.purchase(USER_ID, UUID.randomUUID(), IDEMPOTENCY_KEY.toString());

        assertThat(first).isEqualTo(retry);
        assertThat(gamification.getXpBalance()).isEqualTo(700);
        assertThat(gamification.getTotalXp()).isEqualTo(1_000);
        verify(inventoryRepository).saveAndFlush(any(BuddyInventory.class));
    }

    @Test
    void equipRequiresOwnershipAndMatchingSlotAndSupportsUnequip() {
        when(inventoryRepository.existsByIdUserIdAndIdItemId(USER_ID, ITEM_ID)).thenReturn(true);

        service.equip(USER_ID, MarketSlot.HAT, ITEM_ID);
        verify(equippedRepository).save(any());
        assertThatThrownBy(() -> service.equip(USER_ID, MarketSlot.EYEWEAR, ITEM_ID))
                .isInstanceOf(IllegalArgumentException.class);

        when(inventoryRepository.existsByIdUserIdAndIdItemId(USER_ID, ITEM_ID)).thenReturn(false);
        assertMarketError("NOT_OWNED", HttpStatus.FORBIDDEN,
                () -> service.equip(USER_ID, MarketSlot.HAT, ITEM_ID));
        service.equip(USER_ID, MarketSlot.HAT, null);
        verify(equippedRepository).deleteByUserAndSlot(USER_ID, MarketSlot.HAT);
    }

    @Test
    void chestChoosesAnUnownedCommonAndReturnsEmptyWhenAllAreOwned() {
        MarketItem commonHat = item(MarketSlot.HAT, 250);
        commonHat.setId(UUID.randomUUID());
        MarketItem commonGlasses = item(MarketSlot.EYEWEAR, 200);
        commonGlasses.setId(UUID.randomUUID());
        when(itemRepository.findAvailableByRarity(MarketRarity.COMMON, NOW))
                .thenReturn(List.of(commonHat, commonGlasses));
        when(inventoryRepository.findByIdUserId(USER_ID))
                .thenReturn(List.of(inventory(commonHat.getId(), null, InventorySource.PURCHASE, 250)));

        var reward = service.grantChestItem(USER_ID);

        assertThat(reward).isPresent();
        assertThat(reward.get().id()).isEqualTo(commonGlasses.getId());
        ArgumentCaptor<BuddyInventory> inventoryCaptor = ArgumentCaptor.forClass(BuddyInventory.class);
        verify(inventoryRepository).saveAndFlush(inventoryCaptor.capture());
        assertThat(inventoryCaptor.getValue().getSource()).isEqualTo(InventorySource.CHEST);
        assertThat(inventoryCaptor.getValue().getPricePaid()).isZero();
    }

    @Test
    void chestReturnsEmptyIfEveryCommonIsOwned() {
        MarketItem commonHat = item(MarketSlot.HAT, 250);
        MarketItem commonGlasses = item(MarketSlot.EYEWEAR, 200);
        commonHat.setId(UUID.randomUUID());
        commonGlasses.setId(UUID.randomUUID());
        when(itemRepository.findAvailableByRarity(MarketRarity.COMMON, NOW))
                .thenReturn(List.of(commonHat, commonGlasses));
        when(inventoryRepository.findByIdUserId(USER_ID)).thenReturn(List.of(
                inventory(commonHat.getId(), null, InventorySource.PURCHASE, 250),
                inventory(commonGlasses.getId(), null, InventorySource.PURCHASE, 200)));

        assertThat(service.grantChestItem(USER_ID)).isEmpty();
        verify(inventoryRepository, never()).saveAndFlush(any(BuddyInventory.class));
    }

    private MarketItem item(MarketSlot slot, long price) {
        MarketItem result = new MarketItem();
        result.setId(ITEM_ID);
        result.setSlot(slot);
        result.setName("Hat");
        result.setNameKm("មួក");
        result.setPrice(price);
        result.setRarity(MarketRarity.COMMON);
        result.setAssetKey("hat_straw");
        result.setActive(true);
        return result;
    }

    private BuddyInventory inventory(UUID itemId, UUID key, InventorySource source, long price) {
        BuddyInventory inventory = new BuddyInventory();
        inventory.setId(new BuddyInventoryId(USER_ID, itemId));
        inventory.setIdempotencyKey(key);
        inventory.setSource(source);
        inventory.setPricePaid(price);
        inventory.setAcquiredAt(NOW);
        return inventory;
    }

    private void assertMarketError(String code, HttpStatus status, Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(MarketException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(code);
                    assertThat(exception.getStatus()).isEqualTo(status);
                });
    }
}
