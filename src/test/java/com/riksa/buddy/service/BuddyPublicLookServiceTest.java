package com.riksa.buddy.service;

import com.riksa.buddy.repository.BuddyPublicLookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuddyPublicLookServiceTest {

    @Mock BuddyPublicLookRepository repository;

    @Test
    void batchLoadsAllLooksWithOneRepositoryQuery() {
        UUID userId = UUID.randomUUID();
        BuddyPublicLookRepository.BuddyPublicLookRow row = new BuddyPublicLookRepository.BuddyPublicLookRow() {
            @Override public UUID getUserId() { return userId; }
            @Override public String getSpecies() { return "crocodile"; }
            @Override public String getName() { return "Croco"; }
            @Override public String getSlot() { return "HAT"; }
            @Override public String getAssetKey() { return "hat_straw"; }
        };
        when(repository.findLooksByUserIds(List.of(userId))).thenReturn(List.of(row));

        var looks = new BuddyPublicLookService(repository).getLooks(List.of(userId));

        assertThat(looks.get(userId).species()).isEqualTo("crocodile");
        assertThat(looks.get(userId).equipped()).containsEntry("HAT", "hat_straw");
        verify(repository).findLooksByUserIds(List.of(userId));
    }
}
