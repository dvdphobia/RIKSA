package com.riksa.buddy.service;

import com.riksa.buddy.dto.BuddyResponse;
import com.riksa.buddy.dto.BuddyTodayActionResponse;
import com.riksa.buddy.dto.BuddyUpdateRequest;
import com.riksa.buddy.entity.Buddy;
import com.riksa.buddy.entity.BuddyCareEvent;
import com.riksa.buddy.repository.BuddyCareEventRepository;
import com.riksa.buddy.repository.BuddyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuddyService {

    private static final String DEFAULT_SPECIES = "crocodile";
    private static final String DEFAULT_NAME = "Croco";
    private static final List<String> TODAY_KEYS = List.of(
            BuddyCareRules.LESSON,
            BuddyCareRules.BLOG_READ,
            BuddyCareRules.POST);

    private final BuddyRepository buddyRepository;
    private final BuddyCareEventRepository eventRepository;
    private final BuddyCareRules rules;
    private final Clock buddyCareClock;

    @Transactional
    public Buddy getOrCreate(UUID userId) {
        return ensureAndLock(userId, DEFAULT_SPECIES, DEFAULT_NAME, now());
    }

    @Transactional
    public BuddyResponse getMe(UUID userId) {
        OffsetDateTime now = now();
        Buddy buddy = ensureAndLock(userId, DEFAULT_SPECIES, DEFAULT_NAME, now);
        int settledEnergy = rules.settle(buddy.getEnergy(), buddy.getUpdatedAt(), now);
        return response(buddy, settledEnergy, now);
    }

    @Transactional
    public BuddyResponse update(UUID userId, BuddyUpdateRequest request) {
        if (request.guestEnergy() != null || request.guestBond() != null) {
            Buddy buddy = mergeGuestState(
                    userId, request.species(), request.name(), request.guestEnergy(), request.guestBond());
            OffsetDateTime now = now();
            return response(buddy, buddy.getEnergy(), now);
        }

        OffsetDateTime now = now();
        Buddy buddy = ensureAndLock(userId, DEFAULT_SPECIES, DEFAULT_NAME, now);
        buddy.setEnergy(rules.settle(buddy.getEnergy(), buddy.getUpdatedAt(), now));
        if (request.species() != null) buddy.setSpecies(validateSpecies(request.species()));
        if (request.name() != null && !request.name().trim().isEmpty()) {
            buddy.setName(validateName(request.name()));
        }
        buddy.setUpdatedAt(now);
        buddyRepository.save(buddy);
        return response(buddy, buddy.getEnergy(), now);
    }

    @Transactional
    public Buddy mergeGuestState(
            UUID userId,
            String guestSpecies,
            String guestName,
            Integer guestEnergy,
            Integer guestBond) {
        OffsetDateTime now = now();
        String newSpecies = guestSpecies == null ? DEFAULT_SPECIES : validateSpecies(guestSpecies);
        String newName = guestName == null || guestName.trim().isEmpty()
                ? defaultName(newSpecies)
                : validateName(guestName);
        Buddy buddy = ensureAndLock(userId, newSpecies, newName, now);
        int settledEnergy = rules.settle(buddy.getEnergy(), buddy.getUpdatedAt(), now);
        buddy.setEnergy(Math.max(settledEnergy, guestEnergy == null ? 0 : guestEnergy));
        buddy.setBond(Math.max(buddy.getBond(), guestBond == null ? 0 : guestBond));
        buddy.setUpdatedAt(now);
        return buddyRepository.save(buddy);
    }

    @Transactional
    public void recordCare(UUID userId, String sourceKey, UUID sourceId) {
        String key = BuddyCareRules.normalize(sourceKey);
        BuddyCareRules.Gain gain = rules.gainFor(key);
        OffsetDateTime now = now();
        LocalDate careDay = BuddyCareRules.careDay(now);
        Buddy buddy = ensureAndLock(userId, DEFAULT_SPECIES, DEFAULT_NAME, now);

        if (eventRepository.existsByUserIdAndSourceKeyAndSourceId(userId, key, sourceId)) {
            return;
        }

        long sourceCount = eventRepository.countByUserIdAndSourceKeyAndCareDay(userId, key, careDay);
        Integer cap = rules.dailyCap(key);
        if (cap != null && sourceCount >= cap) {
            return;
        }

        int settledEnergy = rules.settle(buddy.getEnergy(), buddy.getUpdatedAt(), now);
        int bondGain = key.equals(BuddyCareRules.LESSON)
                && sourceCount >= BuddyCareRules.MAX_DAILY_LESSON_BOND_AWARDS
                ? 0
                : gain.bond();
        buddy.setEnergy(Math.min(BuddyCareRules.MAX_ENERGY, settledEnergy + gain.energy()));
        buddy.setBond(buddy.getBond() + bondGain);
        buddy.setUpdatedAt(now);
        buddyRepository.save(buddy);

        BuddyCareEvent event = new BuddyCareEvent();
        event.setUserId(userId);
        event.setSourceKey(key);
        event.setSourceId(sourceId);
        event.setCareDay(careDay);
        event.setEnergyGain(gain.energy());
        event.setBondGain(bondGain);
        event.setCreatedAt(now);
        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<BuddyTodayActionResponse> today(UUID userId) {
        LocalDate careDay = BuddyCareRules.careDay(now());
        return TODAY_KEYS.stream()
                .map(key -> todayAction(userId, key, careDay))
                .toList();
    }

    private BuddyTodayActionResponse todayAction(UUID userId, String key, LocalDate careDay) {
        long count = eventRepository.countByUserIdAndSourceKeyAndCareDay(userId, key, careDay);
        return new BuddyTodayActionResponse(key, count > 0, count, rules.dailyCap(key));
    }

    private BuddyResponse response(Buddy buddy, int energy, OffsetDateTime settledAt) {
        return new BuddyResponse(
                buddy.getSpecies(),
                buddy.getName(),
                energy,
                buddy.getBond(),
                rules.bondLevel(buddy.getBond()),
                settledAt,
                today(buddy.getUserId()));
    }

    private Buddy ensureAndLock(UUID userId, String species, String name, OffsetDateTime now) {
        buddyRepository.insertIfMissing(userId, species, name, now);
        return buddyRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Buddy row could not be created for user " + userId));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(buddyCareClock.instant(), ZoneOffset.UTC);
    }

    private String validateSpecies(String species) {
        String normalized = species.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.equals("crocodile") && !normalized.equals("capybara")) {
            throw new IllegalArgumentException("Buddy species must be crocodile or capybara");
        }
        return normalized;
    }

    private String validateName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 16) {
            throw new IllegalArgumentException("Buddy name must contain between 1 and 16 characters");
        }
        return trimmed;
    }

    private String defaultName(String species) {
        return species.equals("capybara") ? "Capy" : DEFAULT_NAME;
    }
}
