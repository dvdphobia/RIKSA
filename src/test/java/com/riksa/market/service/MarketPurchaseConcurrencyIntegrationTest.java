package com.riksa.market.service;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.gamification.entity.UserGamification;
import com.riksa.gamification.repository.UserGamificationRepository;
import com.riksa.market.entity.MarketItem;
import com.riksa.market.repository.BuddyInventoryRepository;
import com.riksa.market.repository.MarketItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RIKSA_RUN_INTEGRATION_TESTS", matches = "true")
class MarketPurchaseConcurrencyIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired UserGamificationRepository gamificationRepository;
    @Autowired MarketItemRepository itemRepository;
    @Autowired BuddyInventoryRepository inventoryRepository;
    @Autowired MarketService marketService;

    @Test
    void twoConcurrentPurchasesCannotSpendMoreThanTheBalance() throws Exception {
        User user = new User();
        user.setUsername("market-race-" + UUID.randomUUID());
        user.setDisplayName("Market Race User");
        user.setEmail(UUID.randomUUID() + "@example.test");
        user = userRepository.saveAndFlush(user);

        UserGamification gamification = new UserGamification();
        gamification.setUserId(user.getId());
        gamification.setTotalXp(0);
        gamification.setXpBalance(400);
        gamificationRepository.saveAndFlush(gamification);

        List<MarketItem> catalog = itemRepository.findAll();
        UUID strawHat = catalog.stream().filter(item -> item.getAssetKey().equals("hat_straw"))
                .findFirst().orElseThrow().getId();
        UUID sunglasses = catalog.stream().filter(item -> item.getAssetKey().equals("eye_sun"))
                .findFirst().orElseThrow().getId();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        UUID userId = user.getId();
        Future<Boolean> first = pool.submit(() -> purchaseAfterStart(userId, strawHat, ready, start));
        Future<Boolean> second = pool.submit(() -> purchaseAfterStart(userId, sunglasses, ready, start));
        try {
            ready.await();
            start.countDown();
            int successes = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);

            assertThat(successes).isEqualTo(1);
            UserGamification after = gamificationRepository.findByUserId(userId).orElseThrow();
            assertThat(after.getXpBalance()).isBetween(0L, 400L);
            assertThat(inventoryRepository.findByIdUserId(userId)).hasSize(1);
            assertThat(after.getTotalXp()).isZero();
        } finally {
            pool.shutdownNow();
            userRepository.deleteById(userId);
        }
    }

    private boolean purchaseAfterStart(UUID userId, UUID itemId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            marketService.purchase(userId, itemId, UUID.randomUUID().toString());
            return true;
        } catch (MarketException exception) {
            if ("INSUFFICIENT_BALANCE".equals(exception.getCode())) return false;
            throw exception;
        }
    }
}
