package com.riksa.buddy.service;

import com.riksa.buddy.dto.BuddyLookResponse;
import com.riksa.buddy.repository.BuddyPublicLookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuddyPublicLookService {

    private final BuddyPublicLookRepository repository;

    public BuddyLookResponse getLook(UUID userId) {
        return getLooks(List.of(userId)).get(userId);
    }

    public Map<UUID, BuddyLookResponse> getLooks(Collection<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();

        Map<UUID, MutableLook> looks = new LinkedHashMap<>();
        for (BuddyPublicLookRepository.BuddyPublicLookRow row : repository.findLooksByUserIds(userIds)) {
            MutableLook look = looks.computeIfAbsent(row.getUserId(), ignored ->
                    new MutableLook(row.getSpecies(), row.getName(), new LinkedHashMap<>()));
            if (row.getSlot() != null && row.getAssetKey() != null) {
                look.equipped().put(row.getSlot(), row.getAssetKey());
            }
        }

        Map<UUID, BuddyLookResponse> result = new LinkedHashMap<>();
        looks.forEach((id, look) -> result.put(id,
                new BuddyLookResponse(look.species(), look.name(), Map.copyOf(look.equipped()))));
        return result;
    }

    private record MutableLook(String species, String name, Map<String, String> equipped) {}
}
